package pt.isec.amov2526.projetoKotlin.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser

data class ConnectionCodeInfo(
    val code: String,
    val protegeUid: String,
    val protegeName: String,
    val protegeEmail: String,
    val accessLevel: String
)

class ConnectionRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getProteges(monitorUid: String): Result<List<ConnectedUser>> {
        return try {
            val connections = db.collection("users").document(monitorUid)
                .collection("connections")
                .whereEqualTo("role", "Protege")
                .get().await()

            val proteges = connections.map { doc ->
                val uid = doc.getString("uid") ?: ""
                val revoked = doc.getBoolean("revokedByProtege") ?: false
                val userDoc = db.collection("users").document(uid).get().await()
                ConnectedUser(
                    uid = uid,
                    name = userDoc.getString("name") ?: "Unknown",
                    email = userDoc.getString("email") ?: "",
                    role = "Protege",
                    revokedByProtege = revoked
                )
            }
            Result.success(proteges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonitors(protegeUid: String): Result<List<ConnectedUser>> {
        return try {
            val connections = db.collection("users").document(protegeUid)
                .collection("connections")
                .whereEqualTo("role", "Monitor")
                .get().await()

            val monitors = connections.map { doc ->
                val uid = doc.getString("uid") ?: ""
                val accessLevel = doc.getString("accessLevel") ?: "Full Monitoring"
                val userDoc = db.collection("users").document(uid).get().await()
                ConnectedUser(
                    uid = uid,
                    name = userDoc.getString("name") ?: "Unknown",
                    email = userDoc.getString("email") ?: "",
                    role = "Monitor",
                    accessLevel = accessLevel
                )
            }
            Result.success(monitors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeConnection(uid: String, targetUid: String): Result<Unit> {
        return try {
            val batch = db.batch()

            
            val rulesRef = db.collection("users").document(uid)
                .collection("connections").document(targetUid)
                .collection("rules")
            
            val rulesSnapshot = rulesRef.get().await()
            for (doc in rulesSnapshot.documents) {
                batch.delete(doc.reference)
            }

            
            batch.delete(db.collection("users").document(uid).collection("connections").document(targetUid))
            batch.update(db.collection("users").document(uid), "associatedIds", FieldValue.arrayRemove(targetUid))

            
            
            val monitorConnRef = db.collection("users").document(targetUid).collection("connections").document(uid)
            batch.update(monitorConnRef, "revokedByProtege", true)
            

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    suspend fun deleteConnection(monitorUid: String, protegeUid: String): Result<Unit> {
        return try {
            val batch = db.batch()
            
            batch.delete(db.collection("users").document(monitorUid).collection("connections").document(protegeUid))
            batch.update(db.collection("users").document(monitorUid), "associatedIds", FieldValue.arrayRemove(protegeUid))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    suspend fun generateConnectionCode(uid: String, accessLevel: String): Result<String> {
        return try {
            val code = (100000..999999).random().toString() 
            val data = hashMapOf(
                "code" to code,
                "uid" to uid,
                "accessLevel" to accessLevel,
                "timestamp" to Timestamp.now()
            )
            
            db.collection("connection_codes").document(code).set(data).await()
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    suspend fun validateCode(code: String): Result<ConnectionCodeInfo> {
        return try {
            val codeDoc = db.collection("connection_codes").document(code).get().await()
            if (!codeDoc.exists()) {
                throw Exception("Invalid code")
            }

            val timestamp = codeDoc.getTimestamp("timestamp")
            val now = Timestamp.now()
            if (timestamp != null && (now.seconds - timestamp.seconds) > 900) {
                db.collection("connection_codes").document(code).delete()
                throw Exception("Code expired")
            }

            val protegeUid = codeDoc.getString("uid") ?: throw Exception("Invalid Data")
            val accessLevel = codeDoc.getString("accessLevel") ?: "Full Monitoring"

            
            val userDoc = db.collection("users").document(protegeUid).get().await()
            val name = userDoc.getString("name") ?: "Unknown User"
            val email = userDoc.getString("email") ?: ""

            Result.success(ConnectionCodeInfo(code, protegeUid, name, email, accessLevel))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    suspend fun connectWithCode(monitorUid: String, code: String): Result<Unit> {
        return try {
            val codeDoc = db.collection("connection_codes").document(code).get().await()
            if (!codeDoc.exists()) {
                throw Exception("Invalid code")
            }
            
            
            val timestamp = codeDoc.getTimestamp("timestamp")
            val now = Timestamp.now()
            if (timestamp != null && (now.seconds - timestamp.seconds) > 900) {
                db.collection("connection_codes").document(code).delete()
                throw Exception("Code expired")
            }

            val protegeUid = codeDoc.getString("uid") ?: throw Exception("Invalid Data")
            val accessLevel = codeDoc.getString("accessLevel") ?: "Full Monitoring"

            
            if (monitorUid == protegeUid) {
                throw Exception("Cannot connect to yourself")
            }

            
            val monitorDoc = db.collection("users").document(monitorUid).get().await()
            val monitorName = monitorDoc.getString("name") ?: "Monitor"
            
            val protegeDoc = db.collection("users").document(protegeUid).get().await()
            val protegeName = protegeDoc.getString("name") ?: "Protege"

            
            val existingConnection = db.collection("users").document(monitorUid)
                .collection("connections").document(protegeUid)
                .get().await()
            
            if (existingConnection.exists()) {
                throw Exception("User already in your list")
            }

            val batch = db.batch()

            
            val monitorRef = db.collection("users").document(monitorUid)
            batch.update(monitorRef, "associatedIds", FieldValue.arrayUnion(protegeUid))
            
            val monitorConnectionRef = monitorRef.collection("connections").document(protegeUid)
            batch.set(monitorConnectionRef, hashMapOf(
                "uid" to protegeUid,
                "name" to protegeName, 
                "role" to "Protege",
                "since" to FieldValue.serverTimestamp()
            ))

            
            val protegeRef = db.collection("users").document(protegeUid)
            batch.update(protegeRef, "associatedIds", FieldValue.arrayUnion(monitorUid))

            val protegeConnectionRef = protegeRef.collection("connections").document(monitorUid)
            batch.set(protegeConnectionRef, hashMapOf(
                "uid" to monitorUid,
                "name" to monitorName, 
                "role" to "Monitor",
                "accessLevel" to accessLevel,
                "since" to FieldValue.serverTimestamp()
            ))

            
            val codeRef = db.collection("connection_codes").document(code)
            batch.delete(codeRef)

            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}