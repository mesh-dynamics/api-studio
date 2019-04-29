package cubeui.backend

class BootStrap {

    def init = { servletContext ->

        def adminRole = Role.findOrSaveWhere(authority: "ROLE_ADMIN")
        def userRole = Role.findOrSaveWhere(authority: "ROLE_USER")

//        def admin = User.findOrSaveWhere(username: "admin", password: "\$2a\$10\$iet29z0K8HmnZKKvf91hDuWwOjKvsPTgS1.JxI4NpZziOPu3KGIUS")
//        def user = User.findOrSaveWhere(username: "vineetks", password: "\$2a\$10\$cg8c5GTWQt.V1U1wW0bADuynr.rEw2ED3xP24kMUIU/3sWWCFS5SO")
        def admin = User.findOrSaveWhere(username: "admin", password: "admin")
        def user = User.findOrSaveWhere(username: "vineetks", password: "vineetks")
        if (admin.getAuthorities().isEmpty()) {
            UserRole.create(admin, adminRole)
        }
        if (user.getAuthorities().isEmpty()) {
            UserRole.create(user, userRole, true)
        }

        def prod1 = new Product(prodName: "iPhone 7", prodDesc: "New iPhone 7 32GB", prodPrice: 780).save flush:true
        def prod2 = new Product(prodName: "iPhone 7 Plus", prodDesc: "New iPhone 7 Plus 128GB", prodPrice: 990).save flush:true
        def prod3 = new Product(prodName: "iPhone 7 SE", prodDesc: "New iPhone 7 SE 64GB", prodPrice: 520).save flush:true
    }
    def destroy = {
    }
}
