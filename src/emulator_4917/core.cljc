(ns emulator-4917.core
  (:require #?(:cljs [cljs.nodejs :as nodejs])
            #?(:cljs [goog.crypt :as gcrypt])
            [clojure.string :as str]))

;; enable *print-fn* in clojurescript
#?(:cljs (enable-console-print!))

(defrecord State [memory r0 r1 ip is])

(defn make-cpu
  ([& {:keys [memory r0 r1 ip is]
       :or {r0 0
            r1 0
            ip 0
            is 0}}]
   (State. (vec (take 16 (concat memory (repeat 16 0)))) r0 r1 ip is)))

(defn terminate-application!
  "Clojure/Clojurescript wrapper for terminate application."
  []
  #?(:clj
     (System/exit 0)
     :cljs
     (.exit nodejs/process 0)))

;; cmd 0: exit
(defn cmd0
  [{:keys [memory r0 r1 ip is]}]
  (println "Terminate application.")
  (terminate-application!))

;; cmd 1: R0 = R0 + R1
(defn cmd1
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 (+ r0 r1) :r1 r1 :ip (inc ip) :is 1))

;; cmd 2: R0 = R0 - R1
(defn cmd2
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 (- r0 r1) :r1 r1 :ip (inc ip) :is 2))

;; cmd 3: R0 = R0 + 1
(defn cmd3
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 (inc r0) :r1 r1 :ip (inc ip) :is 3))

;; cmd 4: R1 = R1 + 1
(defn cmd4
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 (inc r1) :ip (inc ip) :is 4))

;; cmd 5: R0 = R0 - 1
(defn cmd5
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 (dec r0) :r1 r1 :ip (inc ip) :is 5))

;; cmd 6: R1 = R1 - 1
(defn cmd6
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 (dec r1) :ip (inc ip) :is 6))

;; cmd 7: Ring bell
(defn cmd7
  [{:keys [memory r0 r1 ip is]}]
  (println "Ring the bell!!")
  (make-cpu
   :memory memory :r0 r0 :r1 r1 :ip (inc ip) :is 7))

;; cmd 8: Print <data>
(defn cmd8
  [{:keys [memory r0 r1 ip is]}]
  (println (nth memory (inc ip)))
  (make-cpu
   :memory memory :r0 r0 :r1 r1 :ip (+ ip 2) :is 8))

;; cmd 9: Load value from <data> to R0
(defn cmd9
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r1 r1 :ip (+ ip 2) :is 9
   :r0 (nth memory (nth memory (inc ip)))))

;; cmd 10: Load value from <data> to R1
(defn cmd10
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 r0 :ip (+ ip 2) :is 10
   :r1 (nth memory (nth memory (inc ip)))))

;; cmd 11: Store R0 into <data> position
(defn cmd11
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory (assoc memory (nth memory (inc ip)) r0)
   :r0 r0 :r1 r1 :ip (+ ip 2) :is 11))

;; cmd 12: Store R1 into <data> position
(defn cmd12
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory (assoc memory (nth memory (inc ip)) r1)
   :r0 r0 :r1 r1 :ip (+ ip 2) :is 12))

;; cmd 13: jump to address <data>
(defn cmd13
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 r1 :ip (nth memory (inc ip)) :is 13))

;; cmd 14: jump to address <data> if R0 == 0
(defn cmd14
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 r1
   :ip (if (zero? r0)
         (nth memory (inc ip)) (+ ip 2))
   :is 14))

;; cmd 15: jump to address <data> if R0 != 0
(defn cmd15
  [{:keys [memory r0 r1 ip is]}]
  (make-cpu
   :memory memory :r0 r0 :r1 r1
   :ip (if (zero? r0)
         (+ ip 2)
         (nth memory (inc ip)))
   :is 15))

(defn execute [state]
  (let [curr
        (try (nth (:memory state) (:ip state))
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
    (recur cpu)))

(defn to-4bit-array
  "Convert 0xf4 to [f 4]"
  [s]
  (let [h (bit-and (bit-shift-right s 4) 0x0f) ;; 0xf4 >> 4   => f
        l (bit-and s 0x0f)]                    ;; 0xf4 & 0x0f => 4
    [h l]))

(defn parse-rom
  "Parse binary file and convert contents to vector."
  [file]
  (flatten
   (map to-4bit-array
        #?(:clj
           (with-open [out (java.io.ByteArrayOutputStream.)]
             (clojure.java.io/copy (clojure.java.io/input-stream file) out)
             (.toByteArray out))
           :cljs
           (->  (nodejs/require "fs")
                (.readFileSync file "binary")
                .toString
                gcrypt/stringToByteArray))
        )))

(defn -main [& args]
  (let [arg1 (nth args 0)]
    (if arg1
      (run (make-cpu :memory (parse-rom arg1)))
      (println "Error: Please specify filename."))))

;; setup node.js starter point
#?(:cljs (set! *main-cli-fn* -main))