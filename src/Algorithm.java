import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by jay on 3/29/16.
 */
public class Algorithm {

    private HashMap<Integer, List<Integer>> sparseMap;
    private int supportThreshold;
    private HashMap<String, Integer> allCandidatesWithId;
    private String candidateGenerationType;
    private HashMap<String, Integer> freqItemsetCount;

    public Algorithm(SparseMatrix sparseMatrix, String candidateGenerationType) {
        DataSet dataSet = sparseMatrix.dataSet;
        this.sparseMap = sparseMatrix.getIdVsIsPresentMap();
        this.supportThreshold = 172;
        this.allCandidatesWithId = dataSet.getDistinctItemsets();
        this.candidateGenerationType = candidateGenerationType;
        this.freqItemsetCount = new HashMap<>();
    }

    public HashMap<String, Integer> getFreqItemsetCount() {
        return freqItemsetCount;
    }

    public List<Set<String>> run() {

        int totalFrequentSize = 0;
        int k = 1;
        List<String> maximalItemsets = new ArrayList<>();
        List<String> closedItemsets = new ArrayList<>();
        List<Set<String>> allItemsets = new ArrayList<>();

        System.out.println("************ k = " + k + " ****************");
        System.out.println("Candidates = " + this.allCandidatesWithId.size());
        List<String> freqItemsetsOfSizeOne = getFrequentItemsetsOfSize1(this.allCandidatesWithId.keySet(), k);
        System.out.println("Frequent = " + freqItemsetsOfSizeOne.size());
        totalFrequentSize += freqItemsetsOfSizeOne.size();

        ++k;
        Set<String> candidatesItemsetsFor2 = getCandidateItemsetsForSize2(freqItemsetsOfSizeOne, maximalItemsets, closedItemsets);
        System.out.println("************ k = " + k + " ****************");
        System.out.println("Candidates = " + candidatesItemsetsFor2.size());
        List<Set<String>> freqItemsetsHighK = getFrequentItemsets(candidatesItemsetsFor2, k);
        System.out.println("Frequent = " + (freqItemsetsHighK != null ? freqItemsetsHighK.size() : 0));
        totalFrequentSize += freqItemsetsHighK.size();
        allItemsets.addAll(freqItemsetsHighK);

        while (true) {
            ++k;
            Set<String> candidateItemsets = getCandidateItemsets(freqItemsetsHighK, freqItemsetsOfSizeOne, maximalItemsets, closedItemsets, k);
            System.out.println("************ k = " + k + " ****************");

            System.out.println("Candidates = " + candidateItemsets.size());

            List<Set<String>> tempItemsets = getFrequentItemsets(candidateItemsets, k);

            if (tempItemsets == null || tempItemsets.size() == 0) {
                break;
            } else {
                freqItemsetsHighK.clear();
                freqItemsetsHighK.addAll(tempItemsets);
                System.out.println("Frequent = " + freqItemsetsHighK.size());
                totalFrequentSize += freqItemsetsHighK.size();
                allItemsets.addAll(freqItemsetsHighK);
            }
        }
        System.out.println("********************************");
        System.out.println("Total Maximal Frequent Itemsets = " + maximalItemsets.size());
        System.out.println("Total Closed Frequent Itemsets = " + closedItemsets.size());
        System.out.println("Total Number of Frequent Itemsets = " + totalFrequentSize);
        System.out.println("Actual Frequent Itemsets used for Rule Generation = " + freqItemsetsHighK.size());

        return allItemsets;
    }

    private Set<String> getCandidateItemsetsForSize2(List<String> freqItemsetsOfSizeOne, List<String> maximalItemsets, List<String> closedItemsets) {
        Set<String> size2 = new HashSet<>();

        for (String outerString : freqItemsetsOfSizeOne) {
            List<String> superSets = freqItemsetsOfSizeOne.stream()
                    .filter(innerString -> outerString.compareToIgnoreCase(innerString) < 0)
                    .map(innerString -> String.join(",", outerString, innerString))
                    .collect(Collectors.toList());
            size2.addAll(superSets);

            if (isMaximalFrequent(superSets, 2)) {
                maximalItemsets.add(outerString);
            }

            if (isClosedFrequent(superSets, outerString, 2)) {
                closedItemsets.add(outerString);
            }
        }
        return size2;
    }

    private boolean isClosedFrequent(List<String> superSets, String itemset, int k) {
        int supportCount = getSupportCount(itemset, k - 1);

        return superSets.stream()
                .allMatch(string -> getSupportCount(string, k) < supportCount);
    }

    private boolean isMaximalFrequent(List<String> superSets, int k) {
        return superSets.stream()
                .allMatch(string -> getSupportCount(string, k) <= this.supportThreshold);
    }

    private List<String> getFrequentItemsetsOfSize1(Set<String> allCandidates, int k) {

        return allCandidates.
                stream()
                .filter(string -> getSupportCount(string, k) > this.supportThreshold)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Set<String> getCandidateItemsets(List<Set<String>> freqItemsets, List<String> freqItemsetsOfSizeOne, List<String> maximalItemsets, List<String> closedItemsets, int k) {

        if (this.candidateGenerationType.equals("1")) {
            return candidateKInto1(freqItemsets, freqItemsetsOfSizeOne, maximalItemsets, closedItemsets, k);
        } else {
            return candidateKIntoKMinus1(freqItemsets, maximalItemsets, closedItemsets, k);
        }
    }

    private Set<String> candidateKIntoKMinus1(List<Set<String>> freqItemsets, List<String> maximalItemsets, List<String> closedItemsets, int k) {
        Set<String> candidateItemsetsK = new HashSet<>();
        List<String> superSets = new ArrayList<>();

        for (Set<String> freqItemset : freqItemsets) {

            String freqItemsetsPatternOutside = String.join(",", freqItemset);
            String[] allCandidatesOutside = freqItemsetsPatternOutside.split(",");
            String totalMinusLastOutside = Arrays.stream(allCandidatesOutside)
                    .limit(allCandidatesOutside.length - 1)
                    .collect(Collectors.joining(","));

            String outside = allCandidatesOutside[allCandidatesOutside.length - 1];

            superSets.clear();

            for (Set<String> itemset : freqItemsets) {

                String freqItemsetsPatternInside = String.join(",", itemset);
                String[] allCandidatesInside = freqItemsetsPatternInside.split(",");
                String totalMinusLastInside = Arrays.stream(allCandidatesInside)
                        .limit(allCandidatesInside.length - 1)
                        .collect(Collectors.joining(","));

                String inside = allCandidatesInside[allCandidatesOutside.length - 1];


                if (totalMinusLastOutside.equalsIgnoreCase(totalMinusLastInside) && !outside.equalsIgnoreCase(inside)) {
                    if (inside.compareToIgnoreCase(outside) < 0) {
                        superSets.add(String.join(",", totalMinusLastInside, inside, outside));
                    } else {
                        superSets.add(String.join(",", totalMinusLastInside, outside, inside));
                    }
                }
            }

            candidateItemsetsK.addAll(superSets);

            if (isMaximalFrequent(superSets, k)) {
                maximalItemsets.add(freqItemsetsPatternOutside);
            }

            if (isClosedFrequent(superSets, freqItemsetsPatternOutside, k)) {
                closedItemsets.add(freqItemsetsPatternOutside);
            }
        }

        return candidateItemsetsK;
    }

    private Set<String> candidateKInto1(List<Set<String>> freqItemsetsOfSizeK, List<String> freqItemsetsOfSize1, List<String> maximalItemsets, List<String> closedItemsets, int k) {

        Set<String> candidatesItemsetsK = new HashSet<>();
        for (Set<String> itemset : freqItemsetsOfSizeK) {

            String freqKItemsets = String.join(",", itemset);
            String[] allValues = freqKItemsets.split(",");
            String lastString = allValues[allValues.length - 1];

            List<String> superSets = freqItemsetsOfSize1.stream()
                    .filter(freq1Itemset -> lastString.compareToIgnoreCase(freq1Itemset) < 0)
                    .map(freq1Itemset -> String.join(",", freqKItemsets, freq1Itemset))
                    .collect(Collectors.toList());

            candidatesItemsetsK.addAll(superSets);

            if (isMaximalFrequent(superSets, k)) {
                maximalItemsets.add(freqKItemsets);
            }

            if (isClosedFrequent(superSets, freqKItemsets, k)) {
                closedItemsets.add(freqKItemsets);
            }
        }

        return candidatesItemsetsK;
    }

    private List<Set<String>> getFrequentItemsets(Set<String> allCandidates, int k) {

        if (!allCandidates.isEmpty()) {
            Function<String, Set<String>> convertToSet = string -> {
                Set<String> sortedSet = new TreeSet<>();
                sortedSet.add(string);
                return sortedSet;
            };

            return allCandidates.
                    stream()
                    .filter(string -> getSupportCount(string, k) > this.supportThreshold)
                    .map(convertToSet)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            return null;
        }
    }

    public int getSupportCount(String pattern, int k) {
        if (this.freqItemsetCount.containsKey(pattern)) {
            return this.freqItemsetCount.get(pattern);
        }

        String[] individualItemsets = pattern.split(",");
        int count = 0;
        int internalCount;


        for (Map.Entry<Integer, List<Integer>> transactionsWithId : this.sparseMap.entrySet()) {
            List<Integer> transaction = transactionsWithId.getValue();
            internalCount = 0;

            for (String itemset : individualItemsets) {
                if (transaction.contains(this.allCandidatesWithId.get(itemset))) {
                    internalCount++;
                }
            }
            if (internalCount == k) {
                count++;
            }
        }
        if (count > this.supportThreshold) {
            addToWordCountMap(pattern, count);
        }
        return count;
    }

    private void addToWordCountMap(String string, int count) {
        this.freqItemsetCount.put(string, count);
    }
}
