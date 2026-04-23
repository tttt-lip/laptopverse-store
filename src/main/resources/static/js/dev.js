const DevTools = {
    fillProductForm() {
        document.getElementById('form-name').value = "Laptop " + Math.floor(Math.random() * 1000);
        document.getElementById('form-price').value = (Math.random() * 2000).toFixed(2);
        document.getElementById('form-stock').value = Math.floor(Math.random() * 100);
        document.getElementById('form-specs').value = "CPU i7, 16GB RAM, 512GB SSD";
    },
    fillCategoryForm() {
        document.getElementById('cat-name').value = "Categoria " + Math.floor(Math.random() * 100);
    },
    fillAddress() {
        document.getElementById('shipping-address').value = "Via Roma 123, Milano";
    },
    quickLogin(role) {
        document.getElementById('login-email').value = role === 'admin' ? 'admin@laptopverse.com' : 'customer@laptopverse.com';
        document.getElementById('login-password').value = role === 'admin' ? 'Admin123!' : 'Customer123!';
        App.handleLogin({ preventDefault: () => {} });
    }
};
window.App = { ...window.App, dev: DevTools };
