---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by mike.
--- DateTime: 5/2/20 12:57 AM
---

local RPD = require "scripts/lib/commonClasses"

local gameScene = {}

local prevTime = 0.

--! Called each game logic step
gameScene.onStep = function()

    local time = RPD.Dungeon.hero:localTime()

    if time > prevTime then
        --RPD.glog(string.format("local game time: %6.1f\n", time))
        prevTime = time
    end
end

--! Called when cell should be selected
gameScene.selectCell = function()

end

return gameScene