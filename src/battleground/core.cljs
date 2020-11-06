(ns battleground.core
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
  (->> (string/split csv-string #"[\r\n]+")
       (map #(string/split % #","))
       (csv/process
         {:structs true
          :cast-fns {:hurdle_change csv/->float
                     :vote_differential csv/->int
                     :new_votes csv/->int
                     :precincts_reporting csv/->int
                     :hurdle csv/->float
                     :trailing_candidate_partition csv/->float
                     :votes_remaining (comp - csv/->int)
                     :trailing_candidate_votes csv/->int
                     :hurdle_mov_avg csv/->float
                     :leading_candidate_votes csv/->int
                     :leading_candidate_partition csv/->float
                     :precincts_total csv/->int}})))

(def blue "#1375B7")
(def red  "#C93135")

(defn get-data []
  (go (let [{:keys [body]} (a/<! (http/get "battleground-state-changes.csv"))]
        (->> (process-csv body)
             (group-by :state)))))

(defn update-data []
  (go (reset! app-state (a/<! (get-data)))))

(defn update-loop []
  (go-loop [i 0]
    (js/console.log "Fetching data round:" i)
    (a/<! (update-data))
    (js/console.log "Results for states:" (keys @app-state))
    (a/<! (a/timeout (* 5 60 1000)))
    (recur (inc i))))


(defn results-viz
  [data]
  [oz/vega-lite
   {:data {:values data}
    :width 900
    :height 400
    ;:selection {:grid {:type :interval :bind :scales}}
    :encoding {:x {:field :votes_remaining
                   :type :quantitative
                   :title "Remaining votes"
                   :scale {:zero true}}
               :y {:field :vote_differential
                   :type :quantitative
                   :title "Vote gap"}
               :color {:field :leading_candidate_name
                       :title "Leading candidate"
                       :scale {:domain ["Trump" "Biden"]
                               :range [red blue]}}}
    :layer [{:mark {:type :line
                    :tooltip {:content :data}}}
                    ;:interpolate :step-after}}
             ;:selection {:grid {:type :interval :bind :scales}}}
            {:mark {:type :circle :tooltip {:content :data}}
             :encoding {:size {:field :new_votes
                               :type :quantitative}}
             :selection {:grid {:type :interval :bind :scales}}}]}])

(defn results-table
  [data]
  [oz/data-table
   data
   {}])


(defn intro []
  [:div
   [:h1 "2020 Presidential Election battleground results"]
   [:div
    {:style {:display :none}}
    [:img {:src "sample.png"}]]
   [:p "Made with data from " [:a {:href "https://alex.github.io/nyt-2020-election-scraper/battleground-state-changes.html"}
                               "https://alex.github.io/nyt-2020-election-scraper/battleground-state-changes.html"]
    " (in turn, from the New York Times)."]

   [:p "For source code see " [:a {:href "https://github.com/metasoarous/2020-election-battleground-viz"}
                               "https://github.com/metasoarous/2020-election-battleground-viz"]
       "."]

   [:p "This application will reload data " [:strong "every 5 minutes"]
       ", so no need to refresh over and over.
       However, the features here may improve over time, so feel free to reload periodically."]

   [:p "The visualizations below show the vote advantage of the currently ahead candidate, versus the remaining number of votes.
       These lines and points are colored according to who is ahead at the point in time.
       If you hover over data points, you can see more information about what was reported at that point in time.
       You may also use a mouse scroll wheel to zoom into the visualizations to pick out detail."]

   [:p "If you've found an issue, or would like to make a suggestion or contribution, please see "
       [:a {:href "https://github.com/metasoarous/2020-election-battleground-viz/issues"}
           "https://github.com/metasoarous/2020-election-battleground-viz/issues"]
       "."]

   [:p "Thanks! Please enjoy!"]])

(defn main []
  [:div
   [intro]
   (for [[state updates] @app-state]
     ^{:key state}
     [:div
       [:h2 state]
       [results-viz updates]])])
       ;[results-table updates]])])

(defn init
  [& args]
  (update-loop)
  (rdom/render [main] (.-body js/document)))

