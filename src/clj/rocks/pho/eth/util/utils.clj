(ns rocks.pho.eth.util.utils
  (:require [mount.core :as mount]
            [rocks.pho.eth.config :refer [env]]))

(defn sum-two-dimensional-second [data]
  (reduce #(+ %1 (bigdec (second %2))) 0M data))

(defn deal-depth-tick-data [tick-data ts]
  (let [bids (:bids tick-data)
        asks (:asks tick-data)
        first-bids-price (bigdec (first (first bids)))
        first-asks-price (bigdec (first (first asks)))
        bids-percent1-price (- first-bids-price (/ first-bids-price 100))
        asks-percent1-price (+ first-asks-price (/ first-asks-price 100))
        bids-percent2-price (- first-bids-price (/ first-bids-price 50))
        asks-percent2-price (+ first-asks-price (/ first-asks-price 50))
        bids-percent1 (filter #(>= (first %)
                                   bids-percent1-price)
                              bids)
        asks-percent1 (filter #(<= (first %)
                                   asks-percent1-price)
                              asks)
        bids-percent2 (filter #(and (>= (first %)
                                        bids-percent2-price)
                                    (< (first %)
                                       bids-percent1-price))
                              bids)
        asks-percent2 (filter #(and (<= (first %)
                                        asks-percent2-price)
                                    (> (first %)
                                       asks-percent1-price))
                              asks)
        bids-percent1-amount (sum-two-dimensional-second bids-percent1)
        asks-percent1-amount (sum-two-dimensional-second asks-percent1)
        bids-percent2-amount (sum-two-dimensional-second bids-percent2)
        asks-percent2-amount (sum-two-dimensional-second asks-percent2)]
    {:first-bids-price first-bids-price
     :first-asks-price first-asks-price
     :diff-first-price (- first-asks-price first-bids-price)
     :bids-percent1-amount bids-percent1-amount
     :asks-percent1-amount asks-percent1-amount
     :bids-percent2-amount bids-percent2-amount
     :asks-percent2-amount asks-percent2-amount
     :diff-bids-percent1 (- first-bids-price bids-percent1-price)
     :diff-asks-percent1 (- asks-percent1-price first-asks-price)
     :ts ts}))

(defn format-depth-tick-data [dealed-tick-data]
  (str "\ndatetime:\t" (.toString (java.sql.Timestamp. (:ts dealed-tick-data))) "\n"
       "first bids price:\t" (:first-bids-price dealed-tick-data) "\n"
       "first asks price:\t" (:first-asks-price dealed-tick-data) "\n"
       "diff first price:\t" (:diff-first-price dealed-tick-data) "\n"
       "bids percent1 amount:\t" (:bids-percent1-amount dealed-tick-data) "\n"
       "asks percent1 amount:\t" (:asks-percent1-amount dealed-tick-data) "\n"
       "bids percent2 amount:\t" (:bids-percent2-amount dealed-tick-data) "\n"
       "asks percent2 amount:\t" (:asks-percent2-amount dealed-tick-data) "\n"
       "diff bids percent1:\t" (:diff-bids-percent1 dealed-tick-data) "\n"
       "diff asks percent1:\t" (:diff-asks-percent1 dealed-tick-data)))

(defn cut-trade-detail [time-millis tick-data]
  (let [now-ts (System/currentTimeMillis)
        before-now (- now-ts time-millis)]
    (into (hash-set)
          (filter #(>= (:ts %)
                       before-now)
                  tick-data))))

(defn sort-trade-detail [tick-data]
  (let [lastest-tick (reduce #(if (>= (:ts %1)
                                      (:ts %2))
                                %1
                                %2)
                             (first tick-data) tick-data)
        oldest-tick (reduce #(if (<= (:ts %1)
                                     (:ts %2))
                               %1
                               %2)
                            (first tick-data) tick-data)
        buy (filter #(= (:direction %) "buy")
                    tick-data)
        buy->buy-market (filter #(and (= (:direction %) "buy")
                                      (= (:type %) "buy-market"))
                                tick-data)
        buy->buy-limit (filter #(and (= (:direction %) "buy")
                                     (= (:type %) "buy-limit"))
                               tick-data)
        sell (filter #(= (:direction %) "sell")
                     tick-data)
        sell->sell-market (filter #(and (= (:direction %) "sell")
                                        (= (:type %) "sell-market"))
                                  tick-data)
        sell->sell-limit (filter #(and (= (:direction %) "sell")
                                       (= (:type %) "sell-limit"))
                                 tick-data)
        sum-amount (fn [data]
                     (reduce #(+ %1 (bigdec (:amount %2)))
                             0M data))
        buy-amount (sum-amount buy)
        buy->buy-market-amount (sum-amount buy->buy-market)
        buy->buy-limit-amount (sum-amount buy->buy-limit)
        sell-amount (sum-amount sell)
        sell->sell-market-amount (sum-amount sell->sell-market)
        sell->sell-limit-amount (sum-amount sell->sell-limit)]
    {:lastest-tick lastest-tick
     :oldest-tick oldest-tick
     :diff-price (- (bigdec (:price lastest-tick 0))
                    (bigdec (:price oldest-tick 0)))
     :buy-amount buy-amount
     :buy->buy-market-amount buy->buy-market-amount
     :buy->buy-limit-amount buy->buy-limit-amount
     :sell-amount sell-amount
     :sell->sell-market-amount sell->sell-market-amount
     :sell->sell-limit-amount sell->sell-limit-amount
     :diff-amount (- buy-amount sell-amount)}))

(defn deal-trade-detail [tick-data time-points]
  (loop [tp time-points
         re {}]
    (if (empty? tp)
      re
      (let [one-tp (first tp)
            data (cut-trade-detail (* one-tp 1000)
                                   tick-data)]
        (recur (rest tp)
               (assoc re
                      (keyword (str "sec" one-tp "-size"))
                      (.size data)
                      (keyword (str "sec" one-tp "-status"))
                      (sort-trade-detail data)))))))

(defn format-deal-trade-detail [trade-detail time-points]
  (loop [tp time-points
         re-str ""]
    (if (empty? tp)
      re-str
      (let [one-tp (first tp)
            size ((keyword (str "sec" one-tp "-size")) trade-detail)
            status ((keyword (str "sec" one-tp "-status")) trade-detail)
            diff-price (:diff-price status)
            diff-amount (:diff-amount status)
            buy-amount (:buy-amount status)
            buy->buy-market-amount (:buy->buy-market-amount status)
            buy->buy-limit-amount (:buy->buy-limit-amount status)
            sell-amount (:sell-amount status)
            sell->sell-market-amount (:sell->sell-market-amount status)
            sell->sell-limit-amount (:sell->sell-limit-amount status)]
        (recur (rest tp)
               (str re-str "\n" one-tp " sec size: " size "\n"
                    "diff price: " diff-price
                    "\tdiff amount: " diff-amount
                    "\tbuy: " buy-amount
                    "\tbuy market: " buy->buy-market-amount
                    "\tbuy limit: " buy->buy-limit-amount
                    "\tsell: " sell-amount
                    "\tsell market: " sell->sell-market-amount
                    "\tsell limit: " sell->sell-limit-amount))))))

(mount/defstate raw-data-file-ts :start (.format (java.text.SimpleDateFormat. "yyyy-MM-dd_HH_mm_ss")
                                                 (java.util.Date.)))

(defn log-data [data]
  (let [file-prefix (str (:eth-raw-data-path env "/tmp") "/data.")
        now (.format (java.text.SimpleDateFormat. "yyyy-MM-dd_HH_mm_ss")
                     (java.util.Date.))]
    (when (> (compare (.substring now 0 10)
                      (.substring raw-data-file-ts 0 10))
             0)
      (mount/start-with {#'raw-data-file-ts now}))
    (spit (str file-prefix raw-data-file-ts)
          (str data "\n")
          :append true)))
