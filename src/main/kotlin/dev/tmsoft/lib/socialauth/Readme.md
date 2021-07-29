# Social auth example

## add configuration 
```
    facebook<ManagerPrincipal>(Auth.FacebookManagerAuth) {
        redirectUrl = "http://localhost:3000/api/login/facebook"
        clientId = "44444444444444"
        clientSecret = "55555555555555"
        provider = socialManagerProvider
    }
```

## Configure router
```
route.route("/api/login/facebook") {
    authenticate(Auth.FacebookManagerAuth) {
        get {
            handleLogin(call)
        }
    }
}
```

## Provider example
```
    class SocialManagerProvider @Inject constructor(
        private val transaction: TransactionManager) : SocialProvider<ManagerPrincipal> {
    
        override suspend fun load(email: String?, token: SocialToken): ManagerPrincipal? {
            return transaction {
                val resultRow =
                    ManagerTable.select { ManagerTable.email.address eq email }.singleOrNull()
                resultRow?.let {
                    ManagerPrincipal(UUID.fromString(resultRow[ManagerTable.id].toString()))
                }
            }
        }
    }
```