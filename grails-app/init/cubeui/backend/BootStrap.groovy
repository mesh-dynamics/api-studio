package cubeui.backend

class BootStrap {

    def init = { servletContext ->
        def prod1 = new Product(prodName: "iPhone 7", prodDesc: "New iPhone 7 32GB", prodPrice: 780).save flush:true
        def prod2 = new Product(prodName: "iPhone 7 Plus", prodDesc: "New iPhone 7 Plus 128GB", prodPrice: 990).save flush:true
        def prod3 = new Product(prodName: "iPhone 7 SE", prodDesc: "New iPhone 7 SE 64GB", prodPrice: 520).save flush:true
    }
    def destroy = {
    }
}
