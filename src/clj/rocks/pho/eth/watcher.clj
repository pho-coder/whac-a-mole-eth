(ns rocks.pho.eth.watcher
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]

            [rocks.pho.eth.util.utils :as utils])
  (:import [rocks.pho.eth.util WSClient]))

(mount/defstate raw-data-file-name :start (str "/tmp/data."
                                               (.format (java.text.SimpleDateFormat. "yyyy-MM-dd_HH_mm_ss")
                                                        (java.util.Date.))))
(mount/defstate last-depth-tick-version :start 0)
(mount/defstate last-trade-detail-tick-id :start 0)
(mount/defstate last-trade-detail-data :start (hash-set))

(defn get-ws-client []
  (let [uri "wss://be.huobi.com/ws"
        depth-topic "market.ethcny.depth.percent10"
        depth-id 1000
        trade-detail-topic "market.ethcny.trade.detail"
        trade-detail-id 2000]
    (WSClient. uri depth-topic depth-id trade-detail-topic trade-detail-id)))

(mount/defstate ws-client :start (get-ws-client))

(defn get-new-data []
  (while (> (.size (.queue ws-client)) 0)
    (let [data-str (.take (.queue ws-client))
          data (json/read-str data-str
                              :key-fn keyword)]
      (spit raw-data-file-name
            (str data-str "\n")
            :append true)
      (case (:ch data)
        "market.ethcny.depth.percent10" (let [tick-data (:tick data)
                                              version (:version tick-data)]
                                          (if (> version last-depth-tick-version)
                                            (do (log/info (utils/format-deal-depth-tick-data tick-data))
                                                (mount/start-with {#'last-depth-tick-version version}))
                                            (log/warn "error depth version:" version "last version:" last-depth-tick-version)))
        "market.ethcny.trade.detail" (let [tick-data (:tick data)
                                           id (:id tick-data)]
                                       (if (> id last-trade-detail-tick-id)
                                         (do (doseq [one (:data tick-data)]
                                               (mount/start-with {#'last-trade-detail-data (conj last-trade-detail-data one)}))
                                             (mount/start-with {#'last-trade-detail-tick-id id}))
                                         (log/warn "error trade detail id:" id "last id:" last-trade-detail-tick-id)))
        (log/error data)))))

(defn data-check []
  (get-new-data)
  (log/info  "depth ws client open?:" (.isOpen ws-client)
             "close?:" (.isClosed ws-client)
             "queue size:" (.size (.queue ws-client)))
  (mount/start-with {#'last-trade-detail-data (utils/cut-trade-detail (* 5 60 1000)
                                                                      last-trade-detail-data)})
;  (log/info last-trade-detail-data)
  (log/info "trade detail data size:" (.size last-trade-detail-data))
  (log/info "trade detail:" (utils/format-deal-trade-detail last-trade-detail-data)))
