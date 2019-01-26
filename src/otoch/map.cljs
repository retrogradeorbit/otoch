(ns otoch.map
  (:require [infinitelives.utils.console :refer [log]]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [cljs.core.match :refer-macros [match]]))

(defn key-for [c]
  (case c
    "-" :rocks
    "+" :dirt
    " " :space
    "t" :clover
    "." :grass
    "," :grass-fg
    "X" :grassy
    "C" :cactus-fg
    "c" :cactus
    "R" :reeds-fg
    "r" :reeds))

(defn strs->keymap [strs]
  (mapv #(mapv key-for %) strs))

(defn change-cell [keymap k v]
  (assoc-in keymap k v))

(defn get-tile-at [tm x y]
  (get-in tm [y x]))

(def not-passable? #{:rocks-1 :rocks-2 :rocks-3 :grassy-left :grassy :grassy-right :dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7})
(def passable? (comp not not-passable?))
(def not-walkable? #{:rocks-1 :rocks-2 :rocks-3 :grassy-left :grassy :grassy-right :dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7})
(def walkable? (comp not not-walkable?))

(defn remap [y-1 y y+1]
  (match [y-1 y y+1]
         ;; put top-bottom rocks tiles where the rocks is solo
         [(t :guard #{:stone :ladder-top :ladder :crate :pot :web :space :gold}) :rocks (b :guard #{:stone :ladder-top :ladder :crate :pot :web :space :gold})]
         :rocks-top-bottom

         ;; put bottom rocks tiles where the rocks ends
         [_ :rocks (t :guard #{:stone :ladder-top :ladder :crate :pot :web :space :gold})]
         :rocks-bottom

         ;; put top rocks tiles at the top edges
         [(t :guard #{:stone :ladder-top :ladder :crate :pot :web :space :gold}) :rocks _]
         :rocks-top

         ;; default: dont change tile
         [_ _ _]
         y))

(defn remaph [x-1 x x+1]
  (match [x-1 x x+1]

         ;; grass left edge
         [(_ :guard (complement #{:grassy})) :grassy _]
         :grassy-left

         ;; grass right edge
         [_ :grassy (_ :guard (complement #{:grassy}))]
         :grassy-right

         ;; default leave tile
         [_ _ _]
         x))

(defn mapv-mapv [ss f]
  (mapv (fn [line]
          (mapv (fn [ch]
                  (f ch))
                line))
        ss))

(defn remap-keymap [keymap]
  (let [height (count keymap)
        width (count (first keymap))]
    (mapv (fn [y]
            (mapv (fn [x]
                    (let [top (get-in keymap [(dec y) x])
                          tile (get-in keymap [y x])
                          bottom (get-in keymap [(inc y) x])]
                      (remap top tile bottom)))
                  (range width)))
          (range height))))

(defn remaph-keymap [keymap]
  (let [height (count keymap)
        width (count (first keymap))]
    (mapv (fn [y]
            (mapv (fn [x]
                    (let [left (get-in keymap [y (dec x)])
                          tile (get-in keymap [y x])
                          right (get-in keymap [y (inc x)])]
                      (remaph left tile right)))
                  (range width)))
           (range height))))

(defn randomise-keymap [keymap]
  (let [height (count keymap)
        width (count (first keymap))]
    (mapv (fn [y]
            (mapv (fn [x]
                    (let [
                          tile (get-in keymap [y x])
                          ]
                      (case tile
                        :rocks
                        (rand-nth [:rocks-1 :rocks-2 :rocks-3])

                        :grass
                        (rand-nth [:grass-1 :grass-2 :grass-3])

                        :grass-fg
                        (rand-nth [:grass-fg-1
                                   :grass-fg-2
                                   :grass-fg-3])

                        :dirt
                        (rand-nth
                         [:dirt-1
                          :dirt-2 :dirt-2
                          :dirt-3
                          :dirt-4 :dirt-4
                          :dirt-5
                          :dirt-6 :dirt-6 :dirt-6 :dirt-6 :dirt-6 :dirt-6 :dirt-6
                          :dirt-6 :dirt-6 :dirt-6 :dirt-6 :dirt-6 :dirt-6 :dirt-6
                          :dirt-7 :dirt-7 :dirt-7 :dirt-7 :dirt-7 :dirt-7 :dirt-7
                          :dirt-7 :dirt-7 :dirt-7 :dirt-7 :dirt-7 :dirt-7 :dirt-7

                          ])

                        tile)
                      ))
                  (range width)))
           (range height))))

(defn make-tile-set [resource-key]
  (let [texture (r/get-texture resource-key :nearest)
        tile-lookup
        {
         :rocks-1 [0 0]
         :rocks-2 [(* 1 64) 0]
         :rocks-3 [(* 2 64) 0]
         :dirt-1 [0 64]
         :dirt-2 [(* 1 64) 64]
         :dirt-3 [(* 2 64) 64]
         :dirt-4 [(* 3 64) 64]
         :dirt-5 [(* 4 64) 64]
         :dirt-6 [(* 5 64) 64]
         :dirt-7 [(* 6 64) 64]

         :grassy-left [(* 5 64) 0]
         :grassy [(* 6 64) 0]
         :grassy-right [(* 7 64) 0]
         :clover [(* 3 64) (* 2 64)]
         :grass-1 [(* 0 64) (* 2 64)]
         :grass-2 [(* 1 64) (* 2 64)]
         :grass-3 [(* 2 64) (* 2 64)]
         :grass-fg-1 [(* 0 64) (* 2 64)]
         :grass-fg-2 [(* 1 64) (* 2 64)]
         :grass-fg-3 [(* 2 64) (* 2 64)]
         :cactus [(* 4 64) (* 2 64)]
         :cactus-fg [(* 4 64) (* 2 64)]
         :reeds [(* 5 64) (* 2 64)]
         :reeds-fg [(* 5 64) (* 2 64)]


         }
        ]
    (->> tile-lookup
         (map (fn [[c pos]] [c (t/sub-texture texture pos [64 64])]))
         (into {}))))

(defn make-tiles [tile-set tile-map]
  (filter identity
   (for [row (range (count tile-map))
         col (range (count (first tile-map)))]
     (let [char (nth (tile-map row) col)]
       (when (not= :space char)
         ;;(js/console.log "!" char)
         (s/make-sprite (tile-set char)
                        :x (* 64 col) :y (* 64 row)
                        :xhandle 0 :yhandle 0))))))


(defn make-tiles-struct
  "returns a dict that can be used to tell which nth element of a tileset
is at a location [x y]. keys are positions. values are nth index"
  [tile-set tile-map]
  (into {}
        (let [sprites
              (filter identity
                      (for [row (range (count tile-map))
                            col (range (count (first tile-map)))]
                        (let [char (nth (tile-map row) col)]
                          (when (not= :space char)
                            [col row]))))]
          (map
           (fn [n [x y]]
             [[x y] n])
           (range)
           sprites))))

(def tile-map
  (-> [
       "-                                                                     -------------"
       "-         -            -               ,.  t.                         -------------"
       "- -   -      -                       XXXXXXXXX                        -------------"
       "-        -          -       t ,, R..,                                 -------------"
       "- -            ---          XXXXXXXXXX                                -------------"
       "-       -        - r , .  t                                           -------------"
       "-  -             XXXXXXXXXXXX                                         -------------"
       "-                                                      c    C         -------------"
       "+++++++++    ++++++++++++++++++++++               +++++++++++++++++++++++++++++++++"
       "+++++++++    ++++++++++++++++++++++++++       +++++++++++++++++++++++++++++++++++++"
       "+++++++++    ++++++++++++++++++++          ++++++++++++++++++++++++++++++++++++++++"
       "+++++++++    ++++++++++++          ++++++++++++++++++++++++++++++++++++++++++++++++"
       "+++++++++                   +++++++++++++++++++++++++++++++++++++++++++++++++++++++"
       "+++++++++        ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
       "+++++++++     +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
       ]
      strs->keymap remaph-keymap randomise-keymap
      ))
