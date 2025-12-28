package pt.isec.amov2526.projetoKotlin.data.model

data class ConnectedUser(
    val uid: String,
    val name: String,
    val email: String,
    val role: String,
    val accessLevel: String? = null
)
