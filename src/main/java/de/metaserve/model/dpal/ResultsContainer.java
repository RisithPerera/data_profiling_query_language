package de.metaserve.model.dpal;

public class ResultsContainer {
    
    public Iterable<? extends ResultWrapper> getResults() {
    }

    public boolean isValid(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
    }

    public boolean isSpezable(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
    }

    public boolean isSpezableWithChange(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
    }

    public void spez(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
    }
    

    public ResultWrapper getCounter(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
    }


    public void endCounter(boolean accept, ResultWrapper counter) {
    }

    public boolean reciveCounter(ResultWrapper first, Boolean second, ResultWrapper third, Boolean fourth) {
    }
}
