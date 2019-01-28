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
    "r" :reeds
    "N" :nubby-fg
    "n" :nubby
    "B" :block
    "^" :death-tile-lower
    "V" :death-tile-upper
    "f" :flower))

(defn strs->keymap [strs]
  (mapv #(mapv key-for %) strs))

(defn change-cell [keymap k v]
  (assoc-in keymap k v))

(defn get-tile-at [tm x y]
  (get-in tm [y x]))

(def not-passable? #{:rocks-1 :rocks-2 :rocks-3 :grassy-left :grassy :grassy-right :dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7 :dirt-8 :dirt-9 :dirt-10 :dirt-11 :dirt-top-1 :dirt-top-2 :dirt-top-3 :block-1 :block-2 :block-3 :block-4 :block-5 :dirt-top-left :dirt-top-right :dirt-under-1 :dirt-under-2})
(def passable? (comp not not-passable?))
(def not-walkable? #{:rocks-1 :rocks-2 :rocks-3 :grassy-left :grassy :grassy-right :dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7 :dirt-8 :dirt-9 :dirt-10 :dirt-11 :dirt-top-1 :dirt-top-2 :dirt-top-3 :block-1 :block-2 :block-3 :block-4 :block-5 :dirt-top-left :dirt-top-right :dirt-under-1 :dirt-under-2})
(def walkable? (comp not not-walkable?))

(def all-dirt
  #{:dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7 :dirt-8 :dirt-9 :dirt-10 :dirt-11
    })

(defn remapv [y-1 y y+1]
  (let [res (match [y-1 y y+1]
                   ;; [(_ :guard (complement (conj all-dirt :dirt-top-left :dirt-top-right)))
                   ;;  (_ :guard all-dirt)
                   ;;  (_ :guard (complement (conj all-dirt :dirt-top-left :dirt-top-right)))
                   ;;  ]
                   ;; (rand-nth [:grassy])

                   [(_ :guard (complement (conj all-dirt :dirt-top-left :dirt-top-right)))
                    (_ :guard all-dirt)
                    _]
                   (rand-nth [:dirt-top-1 :dirt-top-2 :dirt-top-3])

[_
                    (_ :guard all-dirt)
                    (_ :guard #(= % :space))
                    ]
                   (rand-nth [:dirt-under-1 :dirt-under-2])

                   ;; default: dont change tile
                   [_ _ _]
                   y)]
    ;;(js/console.log (str y-1) "," (str y) "," (str y+1) "=>" (str res))
    res))

(defn remaph [x-1 x x+1]
  (match [x-1 x x+1]

         ;; grass left edge
         [(_ :guard (complement #{:grassy})) :grassy _]
         :grassy-left

         ;; grass right edge
         [_ :grassy (_ :guard (complement #{:grassy}))]
         :grassy-right

         [:dirt-11
          (_ :guard all-dirt)
          _]
         (rand-nth [:dirt-2 :dirt-4 :dirt-6 :dirt-7 :dirt-8 :dirt-1 :dirt-3 :dirt-5])

         [:dirt-2
          (_ :guard all-dirt)
          _]
         (rand-nth [:dirt-4 :dirt-6 :dirt-7 :dirt-8 :dirt-1 :dirt-3 :dirt-5])

         [:dirt-4
          (_ :guard all-dirt)
          _]
         (rand-nth [:dirt-2 :dirt-6 :dirt-7 :dirt-8 :dirt-1 :dirt-3 :dirt-5])

         ;; default leave tile
         [_ _ _]
         x))

(defn remap [a b c d e f g h i]
  (match [a b c
          d e f
          g h i]

         ;; top left dirt tile
         [_ (_ :guard #(not (#{:dirt :dirt-top-left :dirt-top-right} %))) _
          (_ :guard #(not= :dirt %)) :dirt :dirt
          _ _ _]
         :dirt-top-left

         ;; top right dirt tile
         [_ (_ :guard #(not (#{:dirt :dirt-top-left :dirt-top-right} %))) _
          :dirt :dirt (_ :guard #(not= :dirt %))
          _ _ _]
         :dirt-top-right

         ;; default
         [_ _ _
          _ _ _
          _ _ _]
         e))

(defn mapv-mapv [ss f]
  (mapv (fn [line]
          (mapv (fn [ch]
                  (f ch))
                line))
        ss))

(defn remapv-keymap [keymap]
  (let [height (count keymap)
        width (count (first keymap))]
    (mapv (fn [y]
            (mapv (fn [x]
                    (let [top (get-in keymap [(dec y) x])
                          tile (get-in keymap [y x])
                          bottom (get-in keymap [(inc y) x])]
                      (remapv top tile bottom)))
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

(defn remap-keymap [keymap]
  (let [height (count keymap)
        width (count (first keymap))]
    (mapv (fn [y]
            (mapv (fn [x]
                    (let [a (get-in keymap [(dec y) (dec x)])
                          b (get-in keymap [(dec y) x])
                          c (get-in keymap [(dec y) (inc x)])

                          d (get-in keymap [y (dec x)])
                          e (get-in keymap [y x])
                          f (get-in keymap [y (inc x)])

                          g (get-in keymap [(inc y) (dec x)])
                          h (get-in keymap [(inc y) x])
                          i (get-in keymap [(inc y) (inc x)])
                          ]
                      (remap a b c d e f g h i)))
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

                        :death-tile-upper
                        (rand-nth [:death-tile-upper-1 :death-tile-upper-2])

                        :death-tile-lower
                        (rand-nth [:death-tile-lower-1 :death-tile-lower-2])

                        :flower
                        (rand-nth [:flower-1 :flower-2 :flower-3 :flower-4 :flower-5])

                        :nubby
                        (rand-nth [:nubby-1 :nubby-2 :nubby-3 :nubby-4 :nubby-5])

                        :nubby-fg
                        (rand-nth [:nubby-fg-1 :nubby-fg-2 :nubby-fg-3 :nubby-fg-4 :nubby-fg-5])

                        :block
                        (rand-nth [:block-1 :block-1 :block-1 :block-1 :block-1
                                   :block-1 :block-1 :block-1 :block-1 :block-1
                                   :block-1 :block-1 :block-1 :block-1 :block-1
                                   :block-2 :block-2 :block-2
                                   :block-3 :block-3 :block-3
                                   :block-4 :block-4 :block-4
                                   :block-5 :block-5 :block-5
                                   :rocks-1
                                   :rocks-2
                                   :rocks-3])

                        :dirt
                        (rand-nth
                         [
                          :dirt-9 :dirt-10
                          :dirt-9 :dirt-10
                          :dirt-9 :dirt-10
                          :dirt-9 :dirt-10
                          :dirt-9 :dirt-10
                          :dirt-9 :dirt-10

                          :dirt-1
                          :dirt-2
                          :dirt-3
                          :dirt-4
                          :dirt-5
                          :dirt-6
                          :dirt-7
                          :dirt-8
                          :dirt-11
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
         :dirt-8 [(* 7 64) 64]
         :dirt-9 [(* 8 64) 64]
         :dirt-10 [(* 9 64) 64]
         :dirt-11 [(* 10 64) 64]

         :dirt-under-1 [(* 11 64) 64]
         :dirt-under-2 [(* 12 64) 64]

         :dirt-top-left [(* 8 64) 0]
         :dirt-top-1 [(* 9 64) 0]
         :dirt-top-2 [(* 10 64) 0]
         :dirt-top-3 [(* 11 64) 0]
         :dirt-top-right [(* 12 64) 0]

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
         :nubby-1 [(* 6 64) (* 2 64)]
         :nubby-2 [(* 7 64) (* 2 64)]
         :nubby-3 [(* 8 64) (* 2 64)]
         :nubby-4 [(* 11 64) (* 4 64)]
         :nubby-5 [(* 12 64) (* 4 64)]
         :nubby-1-fg [(* 6 64) (* 2 64)]
         :nubby-2-fg [(* 7 64) (* 2 64)]
         :nubby-3-fg [(* 8 64) (* 2 64)]
         :nubby-4-fg [(* 11 64) (* 4 64)]
         :nubby-5-fg [(* 12 64) (* 4 64)]

         :block-1 [(* 0 64) (* 4 64)]
         :block-2 [(* 1 64) (* 4 64)]
         :block-3 [(* 2 64) (* 4 64)]
         :block-4 [(* 3 64) (* 4 64)]
         :block-5 [(* 4 64) (* 4 64)]

         :death-tile-upper-1 [(* 6 64) (* 4 64)]
         :death-tile-upper-2 [(* 7 64) (* 4 64)]

         :death-tile-lower-1 [(* 8 64) (* 4 64)]
         :death-tile-lower-2 [(* 9 64) (* 4 64)]

         :flower-1 [(* 9 64) (* 2 64)]
         :flower-2 [(* 10 64) (* 2 64)]
         :flower-3 [(* 11 64) (* 2 64)]
         :flower-4 [(* 12 64) (* 2 64)]
         :flower-5 [(* 13 64) (* 2 64)]

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
       "B                                                                                                                                                      ++++++++++++++++++++++++++"
       "B                                                                                              f    ++++                                               ++++++++++++++++++++++++++"
       "B                  ff .ff                                                                ff  +++++++++++  XX       f  f ,           f  f               ++++++++++++++++++++++++++"
       "B ff             XXXXXXXXXXX                                                           +++++++++++++++++          XXXXXXX         XXXXXXXX                 ++++++++++++++++++++++"
       "BXXXXX                        .                                                    +++++++++++++++++++++                       f  ,,                    +++++++++++++++++++++++++"
       "B                            XXX                                                    ++++++++++++++++++++                     XXXXXXXXX               ++++++++++++++++++++++++++++"
       "B             ff tf  .                                                               +++++++++++++++++++                                             ++++++++++++++++++++++++++++"
       "B             XXXXXXXXXX                                                               +++++++++++++++++               B                          t  ++++++++++++++++++++++++++++"
       "B    . f                                                   ,   ,                       +++++++++++++++++              BBB                        ++++++++++++++++++++++++++++++++"
       "B   XXXXXX            f  . fff  . f                       +++++++                      +++++++++++++++++             BBBBB                    n  ++++++++++++++++++++++++++++++++"
       "B                   XXXXXXXXXXXXXXXXXXX            n  ,++++++++++++.                   +++++++++++++++++            BBBBBBB                  ++++++++++++++++++++++++++++++++++++"
       "B                                                  ++++++++++++++++++  ,  n t c        +++++++++++++++++   n ,,,   BBBBBBBBB  c         t++++++++++++++++++++++++++++++++++++++++"
       "B            C   ttt t rr  ,  t    ,            ++++++++++++++++++++++++++++++++++         +++++++++++++++++++++++++++++++++++++       +++++++++++++++++++++++++++++++++++++++++-"
       "B       ..+++++++++++++++++++++++++++++.        +++++++++++++++++++++++++++++++++++++++++     ++++++++++++++++++++++++++++++++++      ++++++++++++++++++++++++++++++++++++++++---"
       "B   C ++++++++++++++++++++++++++++++++++++          +++++++++++++++++++++++++BBBBB++++++++++            . t   .         .  tt      +++++++++++++++++++++++++++++++++++++++++++---"
       "++++++++++++++++++++++++++++++++++++++++++            VVVVVVV+++++++++++++++B     B++++++++++++++++ ++++++++++++++++++++++++++++++++++++++++++++++++++++            ++++++++++---"
       "+++++++++++++++++++                                          VVVV   BBBBBBBB      B++++++++++++++++ ++++++++++++++++++++++++++++++++++++++++++++++                   +++++++++---"
       "+++++++++++++++++++         .                                                     B++++++++++++++++ ++++++++++++++++++++++++++++++++++++++++++++                      ++++++++---"
       "+++++++++++++++++++   +++++++++++..  ,     .     .   ,  .       BBBBBBBBBBBBBBBBBB+++++++++++++++++ +++++++++++++++++++++++++++++++++++++                             ++++++++---"
       "++++++++++++++++      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ +++++++++++++++++++++++++++++                     .,,,...   ++   +++++++++---"
       "++++++++++++++         +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++             ++++++++++++++                             ++++++++++++++   +++++++++---"
       "+++++++++++                 ++++++++++++++++++++++++++++++++++++++++++++++++++++++++           +++  +++                                        ..    +++++++++++++   +++++++++---"
       "++++++++++                      +++++++++++++++++++++++++++++++++++++++++              +++++++++++  +++++++++++++++++++++++     ttt  t  t    +++++++++++++++++++++   +++++++++---"
       "++++++++++                      ++++++++++++++++                             +++++++++++++++++++    +++++++BBBBBBBBBBBBBBBB BBBBBBBBBBBBBBBBBBBBBBBBBBB+++++++++++  ++++++++++---"
       "+++++++          ----                +++++++++++               +++++++++   +++++++++++++++++++++    +++++++BBBBBBBBBBBBBBBB        B    BBBBB       BBB+++++++++++  ++++++++++---"
       "++++++            -                     ++++++++               +++++++++   +++++++++++++++++++      +++++++BBBBBBBBBBBBBBBBBBBBBBB B BB BBBBB BBBBB BBB+++++++++++  ++++++++++---"
       "++++++            -      ----           ++++++++               +++++         +++++++++++++++++      +++++++BBBBBBBBBBBBBBBBBBBBBBB   BB   BBB     B BBB+++++++++    ++++++++++---"
       "++++++++          -                  +++++++++++++++++++++++   +++++         ++++++                ^+++++++BBBBBBBBBBBBBBBB      B BBBBBB BBBBBBB B BBB++++++++     ++++++++++---"
       "+++++++++++++     -            +++++++++++++++++++++++++++++   +++++         ++++++   +++++++++++++++++++++BBBBBBBBBBBBBBB  BBBB B BB     B    BB B                 ++++++++++---"
       "+++++++++++++     +++++++++++++++++++++++++++++++++            ++++++++++++++++++++   +++++++++++++++++++++BBBBBBBBBB      BBBBB BBBB BBBBB BB    BBBBB+++++++++++++++++++++++---"
       "++                +++++++++++++++++++++++                      ++++++++++++++++++++      ++++++++++++++++++BBBBBBBBBBBBBBBBBBBBB            BBBBB    BB+++++++++++++++++++++++---"
       "+++++++++++++     ++++                                         ++++++++++++++++++              ++++++++++++BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB +++++++++++++++++++++++++---"
       "+++++++++++++     ++++                                         +++++++++++++++++               ++++++++++++++++++++++++++++++++++++++++++++++++++++    +++++++++++++++++++++++---"
       "++++++++++++      ++++                                         +++++++++++++++++               ++++++++++++++++++++++++++++++++++++++++++++++++++++    +++++++++++++++++++++++---"
       "++++++++++++       +++                                         ++++++++++++++++                     ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++---"
       "++++++++++++        ++                                         ++++++++++++++++                      ----------------------------------------------------------------------------"
       "+++++++++++         ++                                         ++++++++++++++++                      ----------------------------------------------------------------------------"
       "+++++++++++        +++         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^++++++++++++++++                      ---------------------------------------BBBBBBBBBBBBBBBBBBB------------------"
       "+++++++++++   ++++++++      +++++++++++++++++++++++++++++++++++++++++++++++++++                     ----------------------------------------B                 B------------------"
       "++++++++++    ++++++++    ++++++++++++++++++++++++    nN   trt,. N   n t,,.tN trt,.               ------------------------------------------B                 B------------------"
       "+++++++++++    ++++++   +++++++                     +++++++++++++++++++++++++++++++----      ---------------------------  .    ,, f   ------B                 B------------------"
       "++++++++++++            +++++++   XXXXXX            +++++++++++++++++++++++++++++++------------------------------------  -----------  n       ff,.fnfffftf tf B------------------"
       "++++++     ++++++++++++++++++++                     +++++++++++++++++++++++++++++++------------------------------------  --------------------------------------------------------"
       "++++++ +++  +++++++++++++++++++                        tr    fnf,    n   ...   ++++---------------------------       ,,t   ------------------------------------------------------"
       "++++   ++++   tr,.      n                              ++++++++++++++++++++++   +++-------------------------     XXXXXXXX      --------------------------------------------------"
       "+++  ++++++++++++++++++++++++++N   .,   c  ff , .   ++++++++++++++++++++++++++     ----------------------                r       ------------------------------------------------"
       "+++n +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++  tt,.        C  , N                  XXXXX    ------------------------------------------------"
       "+++B       +++++++++++++++++BBBB+++++++++++++++++++++++++++++++++++++++++++++++++++----------------------                      N t,     N    n,       ---------------------------"
       "+++BBB++++     ++++++          B+++++++++++++++++++++++++++++++++++++++++++++++++++----------------------     N t              XXXXXXXXXX XXXXXXXX      -------------------------"
       "++++++++++++++   rt   ++++++B  B+++++++++++++++++++++++++++++++++++++++++++++++++++---------------------------------                                    -------------------------"
       "++++++++++++++++++++++++++++BBBB+++++++++++++++++++++++++++++++++++++++++++++++++++-----------------------------------                                 --------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++------------------------------------                       XXXXXX  ---------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++------------------------------------ffff  ,ff  f f  ..,t c   r t , ---------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++----------------------------------------------------------------------------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++----------------------------------------------------------------------------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++----------------------------------------------------------------------------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++----------------------------------------------------------------------------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++----------------------------------------------------------------------------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++----------------------------------------------------------------------------------------------"
       "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++----------------------------------------------------------------------------------------------"

       ]
      strs->keymap remap-keymap randomise-keymap remaph-keymap remapv-keymap
      ))
