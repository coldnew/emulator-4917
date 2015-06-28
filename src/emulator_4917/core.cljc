(ns emulator-4917.core
  (:require #?(:cljs [cljs.nodejs :as nodejs])
            #?(:cljs [goog.crypt :as gcrypt])
            [clojure.string :as str]))

;; enable *print-fn* in clojurescript
#?(:cljs (enable-console-print!))

(defn to-4bit-array
  "Convert 0xf4 to [f 4]"
  [s]
  (let [h (bit-shift-right s 4) ;; 0xf4 >> 4   => f
        l (bit-and s 0x0f)]     ;; 0xf4 & 0x0f => 4
    [h l]))

(defn parse-rom
  "Parse binary file and convert contents to vector."
  [file]
  (flatten
   (map to-4bit-array
        #?(:clj
           (.getBytes (slurp file) "ascii")
           :cljs
           (->  (nodejs/require "fs")
                (.readFileSync file "ascii")
                .toString
                gcrypt/stringToUtf8ByteArray)
           ))))

(defn -main [& args]
  (let [arg1 (nth args 0)]
    (if arg1
      (println (parse-rom arg1))
      (println "Error: Please specify filename."))))

;; setup node.js starter point
#?(:cljs (set! *main-cli-fn* -main))