package com.iota.iri.controllers;

import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TipsViewModel {

    public static final int MAX_TIPS = 5000;

    private final FifoHashCache<Hash> tips = new FifoHashCache<>(TipsViewModel.MAX_TIPS);
    private final FifoHashCache<Hash> solidTips = new FifoHashCache<>(TipsViewModel.MAX_TIPS);
    private final Tangle tangle;

    private final SecureRandom seed = new SecureRandom();
    private final Object sync = new Object();

    public TipsViewModel(Tangle tangle) {
        this.tangle = tangle;
    }

    public void addTipHash(Hash hash) {
        synchronized (sync) {
            tips.add(hash);
        }
    }

    public void removeTipHash(Hash hash) {
        synchronized (sync) {
            if (!tips.remove(hash)) {
                solidTips.remove(hash);
            }
        }
    }

    public void setSolid(Hash tip) {
        synchronized (sync) {
            if (tips.remove(tip)) {
                solidTips.add(tip);
            }
        }
    }

    public Set<Hash> getTips() {
        Set<Hash> hashes = new HashSet<>();
        synchronized (sync) {
            Iterator<Hash> hashIterator;
            hashIterator = tips.iterator();
            while (hashIterator.hasNext()) {
                hashes.add(hashIterator.next());
            }

            hashIterator = solidTips.iterator();
            while (hashIterator.hasNext()) {
                hashes.add(hashIterator.next());
            }
        }
        return hashes;
    }

    public List<Hash> getLatestSolidTips(int count) throws Exception {
        List<Hash> result = new ArrayList<>();
        synchronized (sync) {
            if (solidTips.size() == 0) {
                populateSolidTips();
            }

            int i = 0;
            Iterator<Hash> hashIterator = solidTips.descendingIterator();
            while (hashIterator.hasNext() && i < count) {
                result.add(hashIterator.next());
                i++;
            }
        }
        return result;
    }

    public Hash getRandomSolidTipHash() {
        if (solidSize() == 0) {
            return Hash.NULL_HASH;
        }

        synchronized (sync) {
            int index = seed.nextInt(solidTips.size());
            Iterator<Hash> hashIterator;
            hashIterator = solidTips.iterator();
            Hash hash = null;
            while (index-- >= 0 && hashIterator.hasNext()) {
                hash = hashIterator.next();
            }
            return hash;
            //return solidTips.size() != 0 ? solidTips.get(seed.nextInt(solidTips.size())) : getRandomNonSolidTipHash();
        }
    }

    public Hash getRandomNonSolidTipHash() {
        synchronized (sync) {
            int size = tips.size();
            if (size == 0) {
                return null;
            }
            int index = seed.nextInt(size);
            Iterator<Hash> hashIterator;
            hashIterator = tips.iterator();
            Hash hash = null;
            while (index-- >= 0 && hashIterator.hasNext()) {
                hash = hashIterator.next();
            }
            return hash;
            //return tips.size() != 0 ? tips.get(seed.nextInt(tips.size())) : null;
        }
    }

    public int nonSolidSize() {
        synchronized (sync) {
            return tips.size();
        }
    }
    
    public int solidSize() {
        synchronized (sync) {
            return solidTips.size();
        }
    }

    public int size() {
        synchronized (sync) {
            return tips.size() + solidTips.size();
        }
    }

    public void clear() {
        synchronized (sync) {
            tips.clear();
            solidTips.clear();
        }
    }

    private void populateSolidTips() throws Exception {
        HashSet<Hash> visited = new HashSet<>();
  
        // Create a queue for BFS
        Queue<Hash> queue = new LinkedList<>(); 
  
        // Mark the genesis as visited and enqueue it 
        visited.add(Hash.NULL_HASH);
        queue.add(Hash.NULL_HASH); 
  
        Hash currentHash = Hash.NULL_HASH;
        while (!queue.isEmpty()) { 
            currentHash = queue.poll(); 
  
            // Get all approvers, add unvisited to queue and add them to the visited set
            Set<Hash> approvers = ApproveeViewModel.load(tangle, currentHash).getHashes();

            Set<Hash> solidApprovers = new HashSet<>();
            for (Hash approver : approvers) {
                if (TransactionViewModel.fromHash(tangle, approver).isSolid()) {
                    solidApprovers.add(approver);
                }
            }
            
            // Add solid approvers to queue
            for (Hash approver : solidApprovers) {
                if (!visited.contains(approver)) {
                    visited.add(approver);
                    queue.add(approver);
                }
            }

            // If tip, add to solidTips (populate)
            if (solidApprovers.isEmpty()) {
                this.addTipHash(currentHash);
                this.setSolid(currentHash);
            }
        } 
    }

    private class FifoHashCache<K> implements Iterable<K> {

        private final int capacity;
        private final LinkedHashSet<K> set;

        public FifoHashCache(int capacity) {
            this.capacity = capacity;
            this.set = new LinkedHashSet<>();
        }

        public boolean add(K key) {
            int vacancy = this.capacity - this.set.size();
            if (vacancy <= 0) {
                Iterator<K> it = this.set.iterator();
                for (int i = vacancy; i <= 0; i++) {
                    it.next();
                    it.remove();
                }
            }
            return this.set.add(key);
        }

        public boolean remove(K key) {
            return this.set.remove(key);
        }

        public int size() {
            return this.set.size();
        }

        public Iterator<K> iterator() {
            return this.set.iterator();
        }

        public Iterator<K> descendingIterator() {
            LinkedList<K> list = new LinkedList<>(this.set);
            return list.descendingIterator();
        }

        public void clear() {
            set.clear();
        }
    }

}
