(ns otoch.core
  (:require [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.pixelfont :as pf]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.gamepad :as gp]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.async :as async]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.console :refer [log]]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [otoch.line :as line]
            [otoch.map :as tm]
            [otoch.consts :as consts]
            [otoch.controls :as controls]
            [otoch.state :as state]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [otoch.particle :as particle]
            [otoch.enemy :as enemy]
            [otoch.heart :as heart]
            [otoch.game :as game]
            [otoch.titlescreen :as titlescreen]
            [otoch.pickup :as pickup]
            [otoch.utils :as utils]
            [cljs.core.async :refer [chan close! >! <! timeout]]
)
  (:require-macros [cljs.core.async.macros :refer [go]]
                   ;;[infinitelives.pixi.macros :as m]
                   [infinitelives.pixi.pixelfont :as pf]
))

(enable-console-print!)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defonce bg-colour 0x60a0c0)

(def sprite-sheet-layout
  {
   :rune-1 {:pos [(* 3 64) (* 9 64)] :size [64 64]}

   :megalith {:size [64 64] :pos [(* 4 64) (* 2 64)]}

   :enemy-1 {:size [64 64] :pos [(* 64 3) (* 64 7)]}
   :enemy-2 {:size [64 64] :pos [(* 64 5) (* 64 7)]}

   :star-1 {:size [16 16] :pos [64 (* 9 64)]}
   :star-2 {:size [16 16] :pos [(+ 16 64) (* 9 64)]}
   :star-3 {:size [16 16] :pos [(+ 32 64) (* 9 64)]}
   :star-4 {:size [16 16] :pos [(+ 48 64) (* 9 64)]}

   :star-5 {:size [16 16] :pos [64 (+ 16 (* 9 64))]}
   :star-6 {:size [16 16] :pos [(+ 16 64) (+ 16 (* 9 64))]}
   :star-7 {:size [16 16] :pos [(+ 32 64) (+ 16 (* 9 64))]}
   :star-8 {:size [16 16] :pos [(+ 48 64) (+ 16 (* 9 64))]}

   :star-9 {:size [16 16] :pos [64 (+ 32 (* 9 64))]}
   :star-10 {:size [16 16] :pos [(+ 16 64) (+ 32 (* 9 64))]}
   :star-11 {:size [16 16] :pos [(+ 32 64) (+ 32 (* 9 64))]}
   :star-12 {:size [16 16] :pos [(+ 48 64) (+ 32 (* 9 64))]}

   :star-13 {:size [16 16] :pos [64 (+ 48 (* 9 64))]}
   :star-14 {:size [16 16] :pos [(+ 16 64) (+ 48 (* 9 64))]}
   :star-15 {:size [16 16] :pos [(+ 32 64) (+ 48 (* 9 64))]}
   :star-16 {:size [16 16] :pos [(+ 48 64) (+ 48 (* 9 64))]}

   :blood-1 {:size [16 16] :pos [128 (* 9 64)]}
   :blood-2 {:size [16 16] :pos [(+ 16 128) (* 9 64)]}
   :blood-3 {:size [16 16] :pos [(+ 32 128) (* 9 64)]}
   :blood-4 {:size [16 16] :pos [(+ 48 128) (* 9 64)]}

   :blood-5 {:size [16 16] :pos [128 (+ 16 (* 9 64))]}
   :blood-6 {:size [16 16] :pos [(+ 16 128) (+ 16 (* 9 64))]}
   :blood-7 {:size [16 16] :pos [(+ 32 128) (+ 16 (* 9 64))]}
   :blood-8 {:size [16 16] :pos [(+ 48 128) (+ 16 (* 9 64))]}

   :blood-9 {:size [16 16] :pos [128 (+ 32 (* 9 64))]}
   :blood-10 {:size [16 16] :pos [(+ 16 128) (+ 32 (* 9 64))]}
   :blood-11 {:size [16 16] :pos [(+ 32 128) (+ 32 (* 9 64))]}
   :blood-12 {:size [16 16] :pos [(+ 48 128) (+ 32 (* 9 64))]}

   :blood-13 {:size [16 16] :pos [128 (+ 48 (* 9 64))]}
   :blood-14 {:size [16 16] :pos [(+ 16 128) (+ 48 (* 9 64))]}
   :blood-15 {:size [16 16] :pos [(+ 32 128) (+ 48 (* 9 64))]}
   :blood-16 {:size [16 16] :pos [(+ 48 128) (+ 48 (* 9 64))]}

   :heart {:size [128 128] :pos [(* 14 64) (* 3 64)]}

   :beam-1 {:size [64 64] :pos [(* 15 64) (* 6 64)]}
   :beam-2 {:size [64 64] :pos [(* 15 64) (* 8 64)]}
   :beam-3 {:size [64 64] :pos [(* 15 64) (* 10 64)]}

   :rune {:size [64 64] :pos [(* 3 64) (* 9 64)]}

   :tree-1 {:size [64 128] :pos [(* 0 64) (* 11 64)]}
   :tree-2 {:size [64 128] :pos [(* 1 64) (* 11 64)]}
   :tree-3 {:size [128 128] :pos [(* 2 64) (* 11 64)]}
   :tree-4 {:size [128 128] :pos [(* 4 64) (* 11 64)]}
   :tree-5 {:size [192 192] :pos [(* 6 64) (* 10 64)]}
   :tree-6 {:size [128 256] :pos [(* 9 64) (* 9 64)]}
   :tree-7 {:size [64 128] :pos [(* 12 64) (* 11 64)]}
   :tree-8 {:size [64 128] :pos [(* 13 64) (* 11 64)]}

   })


(defonce canvas
  (c/init {:layers [:bg :tilemap :stats :title :ui]
           :background bg-colour
           :expand true
           :origins {:stats :bottom-left
                     :title :top}
           :translate {:stats [40 -40]
                       :title [0 90]}}))

(s/set-default-scale! 1)

(defonce main
  (go
    ;; load image tilesets and sounds
    (let [ogg? (<! (utils/supports-ogg-chan))]
      (<! (r/load-resources canvas :ui ["img/tiles.png"
                                        "img/fonts.png"
                                        "img/title.png"
                                        "img/background-1.png"
                                        "img/background-2.png"
                                        (if ogg? "sfx/collect.ogg" "sfx/collect.mp3")
                                        (if ogg? "sfx/death.ogg" "sfx/death.mp3")
                                        (if ogg? "sfx/jump.ogg" "sfx/jump.mp3")
                                        (if ogg? "sfx/monolith.ogg" "sfx/monolith.mp3")
                                        (if ogg? "sfx/runethrow.ogg" "sfx/runethrow.mp3")
                                        (if ogg? "sfx/thud.ogg" "sfx/thud.mp3")
                                        (if ogg? "music/arabian.ogg" "music/arabian.mp3")])))

    ;; load textures
    (t/load-sprite-sheet! (r/get-texture :tiles) sprite-sheet-layout)
    (t/load-sprite-sheet! (r/get-texture :title)
                          {:title {:pos [0 0]
                                   :size [652 214]}})

    ;; make a number font
    (pf/pixel-font :numbers "img/fonts.png" [10 10] [248 70]
                   :chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#`'.,"
                   :space 5)

    ;; make a small pixelly font
    (pf/pixel-font :pixel "img/fonts.png" [10 110] [240 170]
                   :chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#`'.,"
                   :space 3)

    (sound/play-sound :arabian 0.3 true)

    #_ (go
         (c/with-sprite :title
           [title (pf/make-text :pixel "OTOCH"
                                :scale 4 :xhandle 0.5
                                :tint 0xff8000)

            ]
           (loop []
             (<! (e/next-frame))
             (recur))))

    (let [tile-set (tm/make-tile-set :tiles)]
      (while true
        (<! (titlescreen/run canvas tile-set))
        (<! (game/run canvas tile-set))
        ;;(<! (e/next-frame))
        ))))
