var path = require("path");

module.exports = {
    entry: path.join(__dirname, "src/main/frontend/entry.js"),
    output: {
        filename: "bundle.js"
    }
}