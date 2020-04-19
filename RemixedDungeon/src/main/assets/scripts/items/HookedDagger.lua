---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by mike.
--- DateTime: 4/1/20 8:29 PM
---


local RPD = require "scripts/lib/commonClasses"

local item = require "scripts/lib/item"

return item.init{
    desc  = function ()
        return {
            image     = 3,
            imageFile = "items/daggers.png",
            name      = "HookedDagger_Name",
            info      = "HookedDagger_Info",
            price     = 17,
            equipable = RPD.Slots.weapon
        }
    end,

    goodForMelee = function()
        return true
    end,

    getVisualName = function()
        return "Dagger"
    end,

    slot = function(self, item, belongings)
        if belongings:slotBlocked(RPD.Slots.weapon) then
            return RPD.Slots.leftHand
        end
        return RPD.Slots.weapon
    end,

    accuracyFactor    = function(self, item, user)
        return 1
    end,

    damageRoll        = function(self, item, user)
        local lvl = item:level()
        return math.random(lvl, lvl*2)
    end,

    attackDelayFactor = function(self, item, user)
        return 1
    end,

    attackProc        = function(self, item, attacker, defender, damage)
        RPD.affectBuff(defender,"Bleeding",3)
        return damage
    end,



}