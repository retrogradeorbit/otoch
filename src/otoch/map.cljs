(ns otoch.map
  (:require [infinitelives.utils.console :refer [log]]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [cljs.core.match :refer-macros [match]]))

(defn key-for [c]
  (case c
    "-" :rocks
    "+" :dirt
    "*" :secret-dirt
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
    "f" :flower
    "|" :ladder

    ;; markers for sprites
    "!" :space
    "0" :space
    "1" :space
    "2" :space
    "3" :space

    ;; markers for start locations
    "S" :space
    "H" :space

    ;; TODO remove
    "P" :space
    ))

(defn strs->keymap [strs]
  (mapv #(mapv key-for %) strs))

(defn change-cell [keymap k v]
  (assoc-in keymap k v))

(defn get-tile-at [tm x y]
  (get-in tm [y x]))

(def not-passable? #{:rocks-1 :rocks-2 :rocks-3 :grassy-left :grassy :grassy-right :dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7 :dirt-8 :dirt-9 :dirt-10 :dirt-11 :dirt-top-1 :dirt-top-2 :dirt-top-3 :block-1 :block-2 :block-3 :block-4 :block-5 :dirt-top-left :dirt-top-right :dirt-under-1 :dirt-under-2})
(def passable? (comp not not-passable?))
(def not-walkable? #{:rocks-1 :rocks-2 :rocks-3 :grassy-left :grassy :grassy-right :dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7 :dirt-8 :dirt-9 :dirt-10 :dirt-11 :dirt-top-1 :dirt-top-2 :dirt-top-3 :block-1 :block-2 :block-3 :block-4 :block-5 :dirt-top-left :dirt-top-right :dirt-under-1 :dirt-under-2 :ladder-top})
(def walkable? (comp not not-walkable?))

(def all-dirt
  #{:dirt-1 :dirt-2 :dirt-3 :dirt-4 :dirt-5 :dirt-6 :dirt-7 :dirt-8 :dirt-9 :dirt-10 :dirt-11
    :secret-dirt-1 :secret-dirt-2 :secret-dirt-3 :secret-dirt-4 :secret-dirt-5 :secret-dirt-6 :secret-dirt-7 :secret-dirt-8 :secret-dirt-9 :secret-dirt-10 :secret-dirt-11
     })

(def all-blocks
  #{:block-1 :block-2 :block-3 :block-4 :block-5
    })

(def all-ladders
  #{:ladder-1 :ladder-2 :ladder-top :ladder-bottom}
  )

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
                    (_ :guard (complement (into (conj all-dirt
                                                      :rocks-1 :rocks-2 :rocks-3
                                                      :death-tile-upper-1 :death-tile-upper-2) all-blocks)))
                    ]
                   (rand-nth [:dirt-under-1 :dirt-under-2])

                   [_
                    (_ :guard all-ladders)
                    (_ :guard (complement all-ladders))
                    ]
                   :ladder-bottom

                   [(_ :guard (complement all-ladders))
                    (_ :guard all-ladders)
                    _
                    ]
                   :ladder-top

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
         [_ (_ :guard #(not (#{:dirt :dirt-top-left :dirt-top-right :secret-dirt} %))) _
          (_ :guard #(not (#{:dirt :secret-dirt} %))) :dirt :dirt
          _ _ _]
         :dirt-top-left

         ;; top right dirt tile
         [_ (_ :guard #(not (#{:dirt :dirt-top-left :dirt-top-right :secret-dirt} %))) _
          :dirt :dirt (_ :guard #(not (#{:dirt :secret-dirt} %)))
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
                                   :block-5 :block-5 :block-5])

                        :ladder
                        (rand-nth [:ladder-1 :ladder-2])

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

                        :secret-dirt
                        (rand-nth
                         [
                          :secret-dirt-9 :secret-dirt-10
                          :secret-dirt-9 :secret-dirt-10
                          :secret-dirt-9 :secret-dirt-10
                          :secret-dirt-9 :secret-dirt-10
                          :secret-dirt-9 :secret-dirt-10
                          :secret-dirt-9 :secret-dirt-10

                          :secret-dirt-1
                          :secret-dirt-2
                          :secret-dirt-3
                          :secret-dirt-4
                          :secret-dirt-5
                          :secret-dirt-6
                          :secret-dirt-7
                          :secret-dirt-8
                          :secret-dirt-11
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

         :secret-dirt-1 [0 64]
         :secret-dirt-2 [(* 1 64) 64]
         :secret-dirt-3 [(* 2 64) 64]
         :secret-dirt-4 [(* 3 64) 64]
         :secret-dirt-5 [(* 4 64) 64]
         :secret-dirt-6 [(* 5 64) 64]
         :secret-dirt-7 [(* 6 64) 64]
         :secret-dirt-8 [(* 7 64) 64]
         :secret-dirt-9 [(* 8 64) 64]
         :secret-dirt-10 [(* 9 64) 64]
         :secret-dirt-11 [(* 10 64) 64]

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

         :ladder-top [(* 12 64) (* 5 64)]
         :ladder-1 [(* 12 64) (* 6 64)]
         :ladder-2 [(* 12 64) (* 7 64)]
         :ladder-bottom [(* 12 64) (* 8 64)]

         ;; titlescreen
         :player [0 (* 64 6)]
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

(defn partition-tilemap [tile-map cell-width cell-height]
  (let [map-height (count tile-map)
        map-width (count (first tile-map))]
    (->>
     (for [x (range 0 map-width cell-width)
           y (range 0 map-height cell-height)]
       [
        ;; key
        [x y]

        ;; value
        {:top-left [x y]

         :center [(+ x (/ cell-width 2))
                  (+ y (/ cell-height 2))]

         :tiles
         (mapv
          (fn [yi]
            (mapv (fn [xi] (get-in tile-map [yi xi]))
                  (range x (min (+ x cell-width) map-width))))
          (range y (min (+ y cell-height) map-height)))}

        ])
     (into {}))))

(defn make-container-chunks [tile-set partitioned-map]
  (into {}
        (for [[k {:keys [top-left tiles center] :as chunk}] partitioned-map]
          (let [[x y] top-left]
            [k (assoc chunk :sprites
                      (s/make-container
                       :children (make-tiles tile-set tiles)
                       :x (* 64 x)
                       :y (* 64 y)))]))))

#_
(->
 [[10 11 12 13 14 15 16 17 18 19 10]
  [20 21 22 23 24 25 26 27 28 29 20]
  [30 31 32 33 34 35 36 37 38 39 30]
  [40 41 42 43 44 45 46 47 48 49 40]
  [50 51 52 53 54 55 56 57 58 59 50]
  [60 61 62 63 64 65 66 67 68 69 60]
  [70 71 72 73 74 75 76 77 78 79 70]
  [80 81 82 83 84 85 86 87 88 89 80]
  [90 91 92 93 94 95 96 97 98 99 90]
  [10 11 12 13 14 15 16 17 18 19 10]]
 (partition-tilemap 4 4))


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

(def tile-map-src
  [
   "                                                                                                                                                                                 "
   "                                                                                                                                                                                 "
   "                                                               H                                                                                                                 "
   "                                                                                                                                                                                 "
   "                                                                                                                                                                                 "
   "B              XXXXXXXXXXXXXX                                                                                                                          ++++++++++++++++++++++++++"
   "B      XXXX                          !                                                         f    ++++                                 !             ++++++++++++++++     +++++"
   "B                  ff .ff3                                                        !      ff  +++++++++++  XX       f 3f ,           XXXXXXX             ************    !!  +++++"
   "B ff             XXXXXXXXXXX                                                           +++++++++++++++++          XXXXXXX                               +++++++++++++++     +++++"
   "BXXXXX                        .                                                      +++++++++++++++++++                       f3 ,,                    ++++++++++++++++++ ++++++"
   "B                            XXX                                                      ++++++++++++++++++                     XXXXXXXXX                  ++++++++++++++++++ ++++++"
   "B                                                                                      +++++++++++++++++             ! 2 !                               ++++++            ++++++"
   "B                                                              S                       +++++++++++++++++               B                                 ++++++ +++++++++++++++++"
   "B    . f                                                   +++++++++                   +++++++++++++++++              BBB                                ++++++ +++++++++++++++++"
   "B   XXXXXX            f3 . fff  . f                    ++++++++++++++++                +++++++++++++++++             BBBBB                                +++++          ++++++++"
   "B                   XXXXXXXXXXXXXXXXX              ++++++++++++++++++++++++             +++++++++++++++             BBBBBBB                               +++++++++++ !! ++++++++"
   "B                                               ++++++++++++++++++++++++++++++++          +++++++++++              BBB!!!BBB                              +++++++++++          ++"
   "B            C 3 ttt t rr  ,  t    ,            ++++++++++++++++++++++++++++++++++++                              BBBBB|BBBBB                           ++++++++++++++++++++++|++"
   "B       ..+++++++++++++++++++++++++++++.        +++++++++++++++++++++++++++++++++++++++++++++++++++ ++++++       BBBBBB|BBBBBB                        ++++++++++++++++++++++++|++"
   "B   C ++++++++++++++++++++++++++++++++++++      ++BBBBBBBBBBBBBBBBBB+++++++++++++++++++++++++++++++ ++++++++++  BBBBBBB|BBBBBBB      +++++++++++++++++++++++++++++++++++++++++|++"
   "++|+++++++++++++++++++++++++++++++++++++++      ++B    VVVV        B+++++++++++++++++++++++++++++++ +++++++++++BBBBBBBB|BBBBBBBB++++++++++++++++++++++++            ++++++++++|++"
   "++|++++++++++++++++                            +++B            ! ! B+++++++++++++++++++++++++++++++ ++++++++++BBBBBBBBB|BBBBBBBBB++++++ ! +++++++++++++              +++++++++|++"
   "++|++++++++++++++++         .                  +++B                B+++++++++++++++++++++++++++++++ +++++++++BBBBBBBBBB|BBBBBBBBBB+++++   *************               ++++++++|++"
   "++|++++++++++++++++   +++++++++++..  ,  0  .   +++B            BBBBB+++++++++++++++++++++++++++++++ ++++++++BBBBBBBBBBB|BBBBBBBBBBB+++++++++++++++++++++                      |++"
   "++|+++++++++++++      ++++++++++++++++++++++++++++B     ++     B+++++++++++++++++++++++++++++++++++ +++++++BBBBBBBBBBBB|BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB+++...   ++  |++++++++++++"
   "++|+++++++++++              ++++++++++++++++++++++B     ++     B+++++++++++++++++++++++++                     BBBBBBBBB|BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB+++++++++++  |++++++++++++"
   "++|++++++++                   ++++++++++    ++++++B            B++++++++++++++++++++      +++++++++ +++++++   BB!    BB      BBBBBBBBBBBBBBBBBBBBBBBBBB+++++++++++  |++++++++++++"
   "++|+++++++             2        +++++++  !   ++BBBB                                    ++++++++++++ +++++++BB BBBBB BBBBBBB  !BBBBBBBBBBBBBBBBBBBBBBBBB+++++++++++  |++++++++++++"
   "++|+++++++                         +++++     ++B                            |+++++++++++++++++++    +++++++BB BBB     BBBBB BBBBBBBBBBBBBBBBBBBBBBBBBBB+++++++++++  |++++++++++++"
   "++|++++          ----                +++++++|++B               BBBBBBBBBBB  |+++++++++++++++++++    +++++++BB     BBB !B           B    BBBBB       BBB+++++++++++  |++++++++++++"
   "++|+++   !   2    -             2!     +++++|++B               B+++++++++B  |+++++++++++++++++      +++++++BBBBB BBBBBBB  BBBBBBBB|B BB BBBBB BBBB  BBB+++++++++++  |++++++++++++"
   "++|+++            -      ----          +++++|++B               BBBBBBBBBBB  |+++++++++++++++++      +++++++BBBBB BB       BBBBBBBB|  BB   BBB     B BBB+++++++++    |++++++++++++"
   "++|+++++          -                  +++++++|++B                  BBBB      | +++++VV              ^+++++++BB       BBBBBBB       |BBBBBB BBBBBB  B BBB++++++++     |++++++++++++"
   "++|+++++++++      -     1      +++BBBBB+++++|++B                   BB          ++++   +++++++++++++++++++++BB BBBBBBBBBBBB  BB|B  |BB  !  B    BB B                 |++++++++++++"
   "++|+++++++++      +++++++++++++++B     B+++B|BBB                   BB  ! 2    +++++   +++++++++++++++++++++BB BBB !BB !    BBB|B BBBB BBBBB BB    BBBBB++++++++++++++++++++++++++"
   "++|               ++BBBBBBBBBBBBBB     BBBBB|                      BB+        +++++      ++++++++++++++++++BB      BBBBBBBBBBB|B            BBBB      B++++++++++++++++++++++++++"
   "+++++++++++++     ++BB              ^                              BB+++|  ++++++              ********++++BBBBBBBBBBBBBBBBBBB|BBBBBBBBBBBBBBBBBBBBB  BB+++++++++++++++++++++++++"
   "+++++++++++++    +++BB              B                              BB+++|+ +++++     3       ! ++++++*****++++++++++++++++++++|+++++++++++++++++++B  ! B+++++++++++++++++++++++++"
   "+++   *****      +++BB                                             BB+++|+ +++++  XXXXXX   XXXX+++++++++*******+++++++++++++++|+++++++++++++++++++B    B+++++++++++++++++++++++++"
   "+++ ! +++++        +BB                                             BB+++|  ++++                     +++++++++*****++++++++++++|+++++++++++++++++++BBBBBB+++++++++++++++++++++++++"
   "+++   +++++++       BB        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^BB++   +++++       3              +++++++++++*****         |                                 +++++++++++++++++"
   "+++++++++++++    !  BB        BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB+  +++++++       XXXXXXXX       ++++++++++++++++  !   ++++++++++++++++++++++++++++++++++++|+++++++++++++++++"
   "+++++++++++        +BB        B++++++++++++++++++++++++++++++++++++++  ++++++++     3         3      +++++++++++++++++    ++++++++++++++++++BBBBBBBBBBBBBBBBBBB|+++++++++++++++++"
   "+++++++++++   ++++++BB      +++++++++++++++++++++++++++++++++++++++++  ++++++++   XXXX     XXXXX    ++++++++++++++++++++++++++++++++++++++++B                 B|+++++++++++++++++"
   "++++++++++    ++++++BB    ++++++++++++++++++++++++    nN   trt,. N   n t,,.tN trt,.               ++++++++++++++++++++++++++++++++++++++++++B  !   !   !   !  B|+++++++++++++++++"
   "+++++++++++    +++++B   +++++++    3                |++++++++++++++++++++++++++++++++++      +++++++++++++++++++++++++++  .                                   B|+++++++++++++++++"
   "++++++++++++      0     +++++++   XXX|XXXXX         |++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++  ++++++,, f 3 n   1   ff,3fnfffftf3tf B|+++++++++++++++++"
   "++++++    +++++++++++++++++++++      |   B    !     |++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++  ++++++++++++++++++++++++++++++++++++++|+++++++++++++++++"
   "+++++       ++++++++++++++++++       | XXXXXXXXXXX  |  tr    fnf,    n   ...   +++++++++++++++++++++++++++++++     3 ,,t   ++++++++++++++++++++++++++++++++++++|+++++++++++++++++"
   "++++   +++    tr,.      n            |         B       ++++++++++++++++++++++   ++++++++++++++++++++++++++++     XXXXXXXX      ++++++++++++++++++++++++++++++++|+++++++++++++++++"
   "+++  ++++++++++++++++++++++++++N   .,|  c  ff ,B.   ++++++++++++++++++++++++++     ++++++++++++++++++++++                r       ++++++++++++++++++++++++++++++|+++++++++++++++++"
   "+++n +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++  tt,.        C  , N                  XXXXXXX         +++++++++++++++++++++++|+++++++++++++++++"
   "+++B        ++++++++++++++++BBBBB++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++                        t, 3   N    n,       +++++++++|+++++++++++++++++"
   "+++BBB++++      ++++            B++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++     N t                XXXXXXXX XXXXXXXX      +++++++|+++++++++++++++++"
   "++++++++++++++   rt   ++++++B ! B+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++          B                                |+++++++++++++++++"
   "++++++++++++++++++++++++++++BBBBB+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   2   BBB         !             ++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++     BBBBB             XXXXXX  +++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ffffBBBBBBBf f  ..,t c   r t , +++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   ]
  )

(def tile-map
  (-> tile-map-src strs->keymap remap-keymap randomise-keymap remaph-keymap remapv-keymap))

(defn indexes-of [string char]
  (loop [indexes []
         rem string
         off 0]
    (if-let [i (clojure.string/index-of rem char)]
      (recur (conj indexes (+ i off)) (subs rem (inc i)) (+ 1 i off))
      indexes)))

(defn find-locations [mapsrc char]
  (apply concat
         (map-indexed
          (fn [n l]
            (for [i (indexes-of l char)]
              [i n]))
          mapsrc)))

(defn calculate-rune-locations [mapsrc]
  (find-locations mapsrc "!"))

(def rune-locations
  (calculate-rune-locations tile-map-src)
  )

(def num-runes (count rune-locations))

(defn offset-all [xo yo l]
  (for [[x y] l]
    [(+ x xo) (+ y yo)]))

(defn calculate-enemies [mapsrc]
  {:enemy-0 (offset-all 0.5 0.5 (find-locations mapsrc "0"))
   :enemy-1 (offset-all 0.5 0.5 (find-locations mapsrc "1"))
   :enemy-2 (offset-all 0.5 0.5 (find-locations mapsrc "2"))
   :enemy-3 (offset-all 0.5 0.5 (find-locations mapsrc "3"))
   }
  )


(def enemies
  (calculate-enemies tile-map-src))

(def heart-position
  (vec2/add (vec2/vec2 0.5 0.5)
            (apply vec2/vec2 (first (find-locations tile-map-src "H")))))

(def start-position
  (vec2/add (vec2/vec2 0.5 0.5)
            (apply vec2/vec2 (first (find-locations tile-map-src "S")))))



;;
;; titlescreen tilemaps
;;
(def titlescreen-mapsrc-1
  [
   "+++++++++"
   "+++++++++"
   "+++++++++"])

(def titlescreen-map-1
  (-> titlescreen-mapsrc-1 strs->keymap remap-keymap randomise-keymap remaph-keymap remapv-keymap))

(def titlescreen-mapsrc-2
  [
   "+++++++++++++++"
   "+++++++++++++++"
   "+++++++++++++++"])

(def titlescreen-map-2
  (-> titlescreen-mapsrc-2 strs->keymap remap-keymap randomise-keymap remaph-keymap remapv-keymap))

(def titlescreen-mapsrc-3
  [
   "P"
   "B"])

(def titlescreen-map-3
  (-> titlescreen-mapsrc-3 strs->keymap remap-keymap randomise-keymap remaph-keymap remapv-keymap))
