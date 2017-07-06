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

(defn get-new-depth-data []
  (while true
    (let [data (.take (.queue depth-ws-client))]
      (spit (str "/tmp/data." (.format (java.text.SimpleDateFormat. "yyyy-MM-dd_HH_mm_ss")
                                       (java.util.Date.)))
            data
            :append true))))
