module.exports = {
    entry: "./src/main/resources/webroot/main.jsx",
    output: {
        path: "src/main/resources/webroot",
        filename: "bundle.js"
    },
    module: {
      loaders: [
        { test: /\.jsx$/, loader: 'jsx-loader' }
      ]
    },
    resolve: {
      extensions: ['', '.js', '.jsx']
    }
};
