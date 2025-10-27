package de.metaserve.executor.min.results;

public class ResultsContainer {
    
    public Iterable<? extends ResultWrapper> getResults() {
        return null;
    }

    public boolean isValid(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
        return false;
    }

    public boolean isSpezable(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
        return false;
    }

    public boolean isSpezableWithChange(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
        return false;
    }

    public void spez(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
    }
    

    public ResultWrapper getCounter(ResultWrapper result, Boolean mainSide, Boolean rightSide) {
        return null;
    }


    public void endCounter(boolean accept, ResultWrapper counter) {
    }

    public boolean reciveCounter(ResultWrapper first, Boolean second, ResultWrapper third, Boolean fourth) {
        return false;
    }
}
