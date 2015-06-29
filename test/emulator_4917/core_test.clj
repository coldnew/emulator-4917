(ns emulator-4917.core-test
  (:require [clojure.test :refer :all]
            [emulator-4917.core :refer :all]))

(deftest cpu-test
  (testing "Create cpu vector"
    (is (= (make-cpu)
           (->State (vec (repeat 16 0)) 0 0 0 0))))

  ;; http://stackoverflow.com/questions/29289151/is-there-a-way-to-test-system-exit-in-clojure
  (testing "cmd 0: Terminate application"
    (with-redefs [terminate-application! (constantly "Terminate Application")]
      (is (= "Terminate Application" (cmd0 (make-cpu))))))

  (testing "cmd 1: R0 = R0 + R1"
    (let [r0 5 r1 10
          cpu (make-cpu :r0 r0 :r1 r1)]
      (is (= (:r0 (cmd1 cpu)) (+ r0 r1)))))

  (testing "cmd 2: R0 = R0 - R1"
    (let [r0 10 r1 5
          cpu (make-cpu :r0 r0 :r1 r1)]
      (is (= (:r0 (cmd2 cpu)) (- r0 r1)))))

  (testing "cmd 3: R0 = R0 + 1"
    (let [r0 10
          cpu (make-cpu :r0 r0)]
      (is (= (:r0 (cmd3 cpu)) (+ r0 1)))))

  (testing "cmd 4: R1 = R1 + 1"
    (let [r1 5
          cpu (make-cpu :r1 r1)]
      (is (= (:r1 (cmd4 cpu)) (+ r1 1)))))

  (testing "cmd 5: R0 = R0 - 1"
    (let [r0 5
          cpu (make-cpu :r0 r0)]
      (is (= (:r0 (cmd5 cpu)) (- r0 1)))))

  (testing "cmd 6: R1 = R1 - 1"
    (let [r1 5
          cpu (make-cpu :r1 r1)]
      (is (= (:r1 (cmd6 cpu)) (- r1 1)))))

  (testing "cmd 7: Ring the bell!!"
    (let [r0 10
          r1 5
          cpu (make-cpu :r0 r0 :r1 r1)]
      ;; the result should the same
      ;; TODO: more accurate mock method
      (is (= 7 (:is (cmd7 cpu))))))

  ;; FIXME: how to mock println correctly ?
  (testing "cmd 8: Print <data>"
    (let [cpu (make-cpu :memory [8 5 0])]
      (is (= 8 (:is (cmd8 cpu))))))

  (testing "cmd 9: Load value from <data> to R0"
    (let [cpu (make-cpu :memory [9 0 0])]
      (is (= 9 (:r0 (cmd9 cpu))))))

  (testing "cmd 10: Load value from <data> to R1"
    (let [cpu (make-cpu :memory [9 0 0])]
      (is (= 9 (:r1 (cmd10 cpu))))))

  (testing "cmd 11: Store R0 into <data> position"
    (let [cpu (make-cpu :memory [11 2 0] :r0 5)]
      (is (= [11 2 5] (take 3 (:memory (cmd11 cpu)))))))

  (testing "cmd 12: Store R1 into <data> position"
    (let [cpu (make-cpu :memory [12 2 0] :r1 5)]
      (is (= [12 2 5] (take 3 (:memory (cmd12 cpu)))))))

  (testing "cmd 13: jump to address <data>"
    (let [cpu (make-cpu :memory [13 2 0])]
      (is (= 2 (:ip (cmd13 cpu))))))

  (testing "cmd 14: jump to address <data> if R0 == 0"
    (let [cpu (make-cpu :memory [13 2 0] :r0 0)]
      (is (= 2 (:ip (cmd14 cpu))))))

  (testing "cmd 15: jump to address <data> if R0 != 0"
    (let [cpu (make-cpu :memory [13 2 0] :r0 1)]
      (is (= 2 (:ip (cmd15 cpu)))))))
