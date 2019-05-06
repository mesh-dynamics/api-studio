package cubeui.backend

class BootStrap {

    def init = { servletContext ->

        def adminRole = Role.findOrSaveWhere(authority: "ROLE_ADMIN")
        def userRole = Role.findOrSaveWhere(authority: "ROLE_USER")

        def admin = User.findWhere(username: "admin")
        def user = User.findWhere(username: "vineetks")
        if (admin == null) {
            admin = User.findOrSaveWhere(username: "admin", password: "admin")
            def prod1 = new Product(prodName: "iPhone 7", prodDesc: "New iPhone 7 32GB", prodPrice: 780).save flush:true
            def prod2 = new Product(prodName: "iPhone 7 Plus", prodDesc: "New iPhone 7 Plus 128GB", prodPrice: 990).save flush:true
            def prod3 = new Product(prodName: "iPhone 7 SE", prodDesc: "New iPhone 7 SE 64GB", prodPrice: 520).save flush:true
        }
        if (user == null) {
            user = User.findOrSaveWhere(username: "vineetks", password: "vineetks")
        }
//        def admin = User.findOrSaveWhere(username: "admin", password: "\$2a\$10\$RGvAqjRRfYXf1Mbogmtbjuz9bj3M4G08J8WBezhJEYEBukwucN.8i")
//        def user = User.findOrSaveWhere(username: "vineetks", password: "\$2a\$10\$YRcExtbHlCro2OPrOZRwDuv2NmcmE36Sz4AFBcRvQJ/mLeImoPQq2")
        if (admin.getAuthorities().isEmpty()) {
            UserRole.create(admin, adminRole)
        }
        if (user.getAuthorities().isEmpty()) {
            UserRole.create(user, userRole, true)
        }
    }
    def destroy = {
    }
}
