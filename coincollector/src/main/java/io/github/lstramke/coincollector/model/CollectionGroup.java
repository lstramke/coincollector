package io.github.lstramke.coincollector.model;

import java.util.List;

public class CollectionGroup implements CoinComposite {
    private String id;
    private String name;
    
    @Override
    public String getId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getId'");
    }
    @Override
    public CoinDescription getDescription() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
    }
    @Override
    public Boolean isComplete() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isComplete'");
    }
    @Override
    public int getTotalValue() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTotalValue'");
    }
    @Override
    public int getCoinCount() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCoinCount'");
    }
    @Override
    public List<EuroCoin> getAllCoins() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllCoins'");
    }
    @Override
    public void add(CoinComponent component) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }
    @Override
    public void remove(CoinComponent component) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }
    @Override
    public List<CoinComponent> getChildren() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getChildren'");
    }
    @Override
    public void addCoin(EuroCoin coin) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addCoin'");
    }
    @Override
    public void addCollection(CoinCollection collection) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addCollection'");
    }
    
}
