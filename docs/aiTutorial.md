# AI Tutorial

This guide will teach you how to add a probablistic AI into the game

## Steps

1) Create a new class within the aiPlayer package and give it a name ProbabilisticAiPlayer(Whatever).java.
2) Make this new class extend `ProbabilisticAiPlayer`.
3) In your class set the class variable `mTileProbability` between any value between 0 & 1.  (Cast it to a float as well) -- This needs to be done in `public void onStart()`
4) Set the values of `mAttackBuilding`, `mUpgradeBuilding` & `mSellBuilding`  Make sure they all sum to 1. (They all need to be floats).  Do this in `public void onStart()`

5) If you would like to change how often the AiPlayer works change `mLowerBoundTime` and `mUpperBoundTime` as well withing `onStart()`.
6) At the end of ``onStart()`` run the command `super.onStart()`.  
7) Then you need to create a template.  This needs to be done in the blender GLTF thingy. Basically open network_templates.blend and create a new thing in Collection.  Give it a name.  Then add 2 components.  The Player Class and your AI Player.  If you're stuck tell Nat, I will don't mind helping!
8) Voila you have created a new AI player.  This can be used by the player to give them a different challenge
7) If it does not work tell Nat

## Variable Meanings
- `mTileProbablity` - This is the chance of that the AI player will build a building.  The larger this is the less chance the AI player will try and upgrade or sell or build.
- `mAttackBuilding` - The chance the player will attack if it does not build
- `mUpgradeBuilding` - The chance the player will upgrade one of their stats
- `mSellBuilding` - The chance the player will sell one of their buildings
- `mLowerBoundTime` - The shortest amount of time the player has to wait between each of their moves
- `mUpperBoundTime` - The longest amount of time the player has to wait between each of their moves.  

## How AI works
This AI firstly needs to check whether they can play game.  This is done by creating a random time and then getting it in between `mLowerBoundTime` and `mUpperBoundTime` inclusive.  This is updated after every time the player plays.  After checking it then needs to decide what action to do.  Firstly it checks how many buildings it has.  If only one it creates a new Building (Why.  Because I chose to.).  If it has built it then needs to decide whether to build or to do something with a building.  If building it chooses a legal tile and tries to build.  If something with a building it then needs to decide whether to attack, upgrade a Stat or Sell a building and then from their tries that action.

Any questions please ask Nat 

## Current Numbers
Here are what all the variables which you need to worry about are set at 

- **BaseAI**
	- `mLowerBoundTime` = 1
	- `mUpperBoundTime` = 2
- **ProbabilisticAiPlayer** 
	- `mTileProbability` = 0.5
		- By this definition `mBuildingProbability` = 0.5
	- `mAttackBuilding` = 0.5
	- `mUpgradeBuilding` = 0.2
	- `mSellBuilding` = 0.1