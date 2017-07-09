(ns rocks.pho.eth.util.utils)

(defn sum-two-dimensional-second [data]
  (reduce #(+ %1 (bigdec (second %2))) 0M data))

(defn deal-depth-tick-data [tick-data]
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
     :diff-asks-percent1 (- asks-percent1-price first-asks-price)}))

(defn format-deal-depth-tick-data [tick-data]
  (let [re (deal-depth-tick-data tick-data)]
    (str "\nfirst bids price:\t" (:first-bids-price re) "\n"
         "first asks price:\t" (:first-asks-price re) "\n"
         "diff first price:\t" (:diff-first-price re) "\n"
         "bids percent1 amount:\t" (:bids-percent1-amount re) "\n"
         "asks percent1 amount:\t" (:asks-percent1-amount re) "\n"
         "bids percent2 amount:\t" (:bids-percent2-amount re) "\n"
         "asks percent2 amount:\t" (:asks-percent2-amount re) "\n"
         "diff bids percent1:\t" (:diff-bids-percent1 re) "\n"
         "diff asks percent1:\t" (:diff-asks-percent1 re))))

(defn deal-trade-detail [tick-data]
  (let [now-ts (System/currentTimeMillis)
        before-now (- now-ts (* 5 60 1000))]
    (into (hash-set)
          (filter #(>= (:ts %)
                       before-now)
                  tick-data))))
