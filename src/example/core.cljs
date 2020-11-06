(ns example.core
  (:require [oz.core :as oz]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs-http.client :as http]
            [semantic-csv.core :as csv]
            [clojure.string :as string]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defonce app-state (r/atom {}))

(defn process-csv
  [csv-string]
  (->> (string/split csv-string #"\r\n")
       (map #(string/split % #","))
       (csv/process
         {:cast-fns {:hurdle_change csv/->float
                     :vote_differential csv/->int
                     :new_votes csv/->int
                     :precincts_reporting csv/->int
                     :hurdle csv/->float
                     :trailing_candidate_partition csv/->float
                     :votes_remaining csv/->int
                     :trailing_candidate_votes csv/->int
                     :hurdle_mov_avg csv/->float
                     :leading_candidate_votes csv/->int
                     :leading_candidate_partition csv/->float
                     :precincts_total csv/->int}})))

(defn get-data []
  (go (let [{:keys [body]} (a/<! (http/get "battleground-state-changes.csv"))]
        (->> (process-csv body)
             (group-by :state)))))


(keys @app-state)
(defn update-data []
  (go (reset! app-state (a/<! (get-data)))))


(defn update-loop []
  (go-loop [i 0]
    (js/console.log "Fetching data round:" i)
    (a/<! (update-data))
    ;(a/<! (a/timeout (* 5 60 1000)))
    (a/<! (a/timeout (* 30 1000)))
    (recur (inc i))))


(defn results-viz
  [data]
  [oz/vega-lite
   {:data {:values data}
    :width 600
    :height 400
    :encoding {:x {:field :timestamp
                   :type :temporal}
               :y {:field :vote_differential
                   :type :quantitative}
               :color {:field :leading_candidate_name
                       :scale {:domain ["Trump" "Biden"]
                               :range [:red :blue]}}}
    :layer [{:mark {:type :line :tooltip {:content :data}}}
            {:mark {:type :point :tooltip {:content :data}}}]}])


(defn main []
  [:div
   [:h1 "Election results viz"]
   [:p "Made with data from " [:a {:href "https://alex.github.io/nyt-2020-election-scraper/battleground-state-changes.html"}
                               "https://alex.github.io/nyt-2020-election-scraper/battleground-state-changes.html"]]

   [:p "For source code see " [:a {:href "https://github.com/metasoarous/2020-election-battleground-viz"}
                               "https://github.com/metasoarous/2020-election-battleground-viz"]]

   (for [[state updates] @app-state]
     ^{:key state}
     [:div
       [:h2 state]
       [results-viz updates]])])

(defn init
  [& args]
  (update-loop)
  (rdom/render [main] (.-body js/document)))

