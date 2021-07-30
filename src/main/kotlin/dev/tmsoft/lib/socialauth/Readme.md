# Social auth example

## add configuration 
```
    facebook(Auth.FacebookManagerAuth) {
        redirectUrl = "http://localhost:3000/api/login/facebook"
        clientId = "44444444444444"
        clientSecret = "55555555555555"
    }
```

## Configure router
```
    route.route("/api/login/facebook") {
        authenticateBySocial<ManagerPrincipal>(
            Auth.FacebookManagerAuth,
            FacebookTransformer(socialManagerProvider),
        ) {
            get {
                handleSocialLogin(call)
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