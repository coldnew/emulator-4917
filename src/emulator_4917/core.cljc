(ns emulator-4917.core
  (:require #?(:cljs [cljs.nodejs :as nodejs])
            #?(:cljs [goog.crypt :as gcrypt])
            [clojure.string :as str])
  #?(:clj (:gen-class)))

;; enable *print-fn* in clojurescript
#?(:cljs (enable-console-print!))

(defrecord State [memory r0 r1 pc is])

(defn make-cpu
  ([& {:keys [memory r0 r1 pc is]
       :or {r0 0
            r1 0
            pc 0
            is 0}}]
   (State. (vec (take 16 (concat memory (repeat 16 0)))) r0 r1 pc is)))

(defn terminate-application!
  "Clojure/Clojurescript wrapper for terminate application."
  []
  #?(:clj
     (System/exit 0)
     :cljs
     (.exit nodejs/process 0)))

(defn cmd0
  "cmd 0: exit application."
  [{:keys [memory r0 r1 pc is]}]
  (println "Terminate application.")
  (terminate-application!))

(defn cmd1
  "cmd 1: R0 = R0 + R1"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 (+ r0 r1) :r1 r1 :pc (inc pc) :is 1))

(defn cmd2
  "cmd 2: R0 = R0 - R1"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 (- r0 r1) :r1 r1 :pc (inc pc) :is 2))

(defn cmd3
  "cmd 3: R0 = R0 + 1"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 (inc r0) :r1 r1 :pc (inc pc) :is 3))

(defn cmd4
  "cmd 4: R1 = R1 + 1"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 (inc r1) :pc (inc pc) :is 4))

(defn cmd5
  "cmd 5: R0 = R0 - 1"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 (dec r0) :r1 r1 :pc (inc pc) :is 5))

(defn cmd6
  "cmd 6: R1 = R1 - 1"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 (dec r1) :pc (inc pc) :is 6))

(defn cmd7
  "cmd 7: Ring bell"
  [{:keys [memory r0 r1 pc is]}]
  (println "Ring the bell!!")
  (make-cpu
   :memory memory :r0 r0 :r1 r1 :pc (inc pc) :is 7))

(defn cmd8
  "cmd 8: Print <data>"
  [{:keys [memory r0 r1 pc is]}]
  (println (nth memory (inc pc)))
  (make-cpu
   :memory memory :r0 r0 :r1 r1 :pc (+ pc 2) :is 8))

(defn cmd9
  "cmd 9: Load value from <data> to R0"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r1 r1 :pc (+ pc 2) :is 9
   :r0 (nth memory (nth memory (inc pc)))))

(defn cmd10
  "cmd 10: Load value from <data> to R1"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 r0 :pc (+ pc 2) :is 10
   :r1 (nth memory (nth memory (inc pc)))))

(defn cmd11
  "cmd 11: Store R0 into <data> position"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory (assoc memory (nth memory (inc pc)) r0)
   :r0 r0 :r1 r1 :pc (+ pc 2) :is 11))

(defn cmd12
  "cmd 12: Store R1 into <data> position"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory (assoc memory (nth memory (inc pc)) r1)
   :r0 r0 :r1 r1 :pc (+ pc 2) :is 12))

(defn cmd13
  "cmd 13: jump to address <data>"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 r1 :pc (nth memory (inc pc)) :is 13))

(defn cmd14
  "cmd 14: jump to address <data> if R0 == 0"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 r1
   :pc (if (zero? r0)
         (nth memory (inc pc)) (+ pc 2))
   :is 14))

(defn cmd15
  "cmd 15: jump to address <data> if R0 != 0"
  [{:keys [memory r0 r1 pc is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 r1
   :pc (if (zero? r0)
         (+ pc 2)
         (nth memory (inc pc)))
   :is 15))

(defn execute [state]
  (let [curr
        (try (nth (:memory state) (:pc state))
             (catch
                 #?(:clj
                    Exception
                    :cljs
                    js/Error) _ 0))]
    ;; (println state)
    (case curr
      ;; 1-byte instruction
      1 (cmd1 state)
      2 (cmd2 state)
      3 (cmd3 state)
      4 (cmd4 state)
      5 (cmd5 state)
      6 (cmd6 state)
      7 (cmd7 state)
      ;; 2-byte instruction
      8 (cmd8 state)
      9 (cmd9 state)
      10 (cmd10 state)
      11 (cmd11 state)
      12 (cmd12 state)
      13 (cmd13 state)
      14 (cmd14 state)
      15 (cmd15 state)
      ;; default
      (cmd0 state))))

(defn run [command]
  (let [cpu (execute command)]
    ;; (print cpu)
    (recur cpu)))

(defn to-4bit-array
  "Convert 0xf4 to [f 4]"
  [s]
  (let [h (bit-and (bit-shift-right s 4) 0x0f) ;; 0xf4 >> 4   => f
        l (bit-and s 0x0f)]                    ;; 0xf4 & 0x0f => 4
    [h l]))

(defn parse-binary-file
  [file]
  #?(:clj
     (with-open [out (java.io.ByteArrayOutputStream.)]
       (clojure.java.io/copy (clojure.java.io/input-stream file) out)
       (.toByteArray out))
     :cljs
     (->  (nodejs/require "fs")
          (.readFileSync file "binary")
          .toString
          gcrypt/stringToByteArray)))

(defn parse-rom
  "Parse binary file and convert contents to vector."
  [file]
  (flatten
   (map to-4bit-array (parse-binary-file file))))

(defn -main [& args]
  (let [arg1 (nth args 0)]
    (if arg1
      (run (make-cpu :memory (parse-rom arg1)))
      (println "Error: Please specify filename."))))

;; setup node.js starter point
#?(:cljs (set! *main-cli-fn* -main))