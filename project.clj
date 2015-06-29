(defproject emulator-4917 "0.1.0-SNAPSHOT"
  :description "4917 emulator for clojure JVM and clojurescript node.js"
  :url "https://github.com/coldnew/emulator-4917"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]]

  :plugins [[lein-cljsbuild "1.0.6"]]

  :min-lein-version "2.5.1"

  :cljsbuild {:builds
              [{
                :source-paths ["src"]
                :compiler {:output-to "target/emulator-4917.js"
                           :output-dir "target"
                           :source-map "target/emulator-4917.js.map"
                           :target :nodejs
                           :optimizations :none
                           :pretty-print true}}]}
  :aot [emulator-4917.core]
  :main emulator-4917.core)