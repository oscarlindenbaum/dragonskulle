/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

public class AttackStat extends Stat<Double> {

    private double normalise(int level) {
        double min = Double.valueOf(LEVEL_MIN);
        double max = Double.valueOf(LEVEL_MAX);
        double value = Double.valueOf(level);

        return (value - min) / (max - min);
    }

    @Override
    protected Double levelToValue() {
        System.out.println(normalise(mLevel));

        switch (mLevel) {
            case 0:
                return 0d;
            case 1:
                return 0.2;
            case 2:
                return 0.4;
            case 3:
                return 0.6;
            case 4:
                return 0.8;
            case 5:
                return 1d;
            default:
                return 0d;
        }
    }
}
