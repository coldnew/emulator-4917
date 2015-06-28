// http://stackoverflow.com/questions/25803420/how-to-compile-clojurescript-to-nodejs
try {
    require("source-map-support").install();
} catch(err) {
}
require("./target/goog/bootstrap/nodejs.js");
require("./target/emulator-4917.js");
require("./target/emulator_4917/core");
emulator_4917.core._main(process.argv[2]); // passing argument