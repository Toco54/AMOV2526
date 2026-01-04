package pt.isec.amov2526.projetoKotlin.data.model

/**
 * Modelo de apresentação para um utilizador conectado (Monitor ou Protegido).
 *
 * Utilizado nas listas de gestão de conexões.
 *
 * @property revokedByProtege Se true, indica uma "Soft Deletion": A conexão existe mas está inativa/revogada pelo lado do protegido.
 * @property accessLevel Nível de permissões do monitor (ex: "Full Monitoring", "Alerts Only").
 */
data class ConnectedUser(
    val uid: String,
    val name: String,
    val email: String,
    val role: String,
    val accessLevel: String? = null,
    val revokedByProtege: Boolean = false
)