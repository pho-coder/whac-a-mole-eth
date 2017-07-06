(ns rocks.pho.eth.watcher
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json])
  (:import [rocks.pho.eth.utils WSClient]))

(defn health-check [])

(defn get-depth-ws-client []
  (let [uri "wss://be.huobi.com/ws"
        topic "market.ethcny.depth.step1"
        id 1000]
    (WSClient. uri topic id)))

(mount/defstate depth-ws-client :start (get-depth-ws-client))

(defn prn-data [data]
  (let [d (json/read-str data
                         :key-fn keyword)]
    (prn)
    (prn (str "ch: " (:ch d)))
    (prn (str "ts: " (.toString (java.sql.Timestamp. (:ts d)))))
    (prn (str "tick id: " (:id (:tick d))))
    (prn (str "tick ts: " (.toString (java.sql.Timestamp. (:ts (:tick d))))))
    (prn (str "first bids price: " (first (first (:bids (:tick d))))))))

(defn get-new-depth-data []
  (let [file-name (str "/tmp/data."
                       (.format (java.text.SimpleDateFormat. "yyyy-MM-dd_HH_mm_ss")
                                (java.util.Date.)))]
    (while true
     (let [data (.take (.queue depth-ws-client))]
       (spit file-name
             (str data "\n")
             :append true)
       (prn-data data)))))
