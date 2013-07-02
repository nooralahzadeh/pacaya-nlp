package edu.jhu.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.util.Alphabet;

public class DepTree implements Iterable<DepTreeNode> {

    public static final int EMPTY_POSITION = -2;
    
    protected List<DepTreeNode> nodes = new ArrayList<DepTreeNode>();
    protected int[] parents;
    protected boolean isProjective;
    
    protected DepTree() {
        // Only for subclasses.
    }
    
    /**
     * Construct a dependency tree from a sentence and the head of each token.
     * 
     * @param sentence The input sentence.
     * @param parents The index of the parent of each token. -1 indicates the root.
     * @param isProjective Whether the tree is projective.
     */
    public DepTree(Sentence sentence, int[] parents, boolean isProjective) {
        this.isProjective = isProjective;
        this.parents = parents;
        nodes.add(new WallDepTreeNode());
        for (int i=0; i<sentence.size(); i++) {
            Label label = sentence.get(i);
            nodes.add(new NonprojDepTreeNode(label, i));
        }
        // Add parent/child links to DepTreeNodes
        addParentChildLinksToNodes();
    }
    
    /**
     * Construct a dependency tree from a wall node and its children.
     * 
     * @param wall
     */
    @SuppressWarnings("unchecked")
    public DepTree(ProjDepTreeNode wall) {
        isProjective = true;
        nodes = (List<DepTreeNode>)wall.getInorderTraversal();
        // Set all the positions on the nodes
        int position;
        position=WallDepTreeNode.WALL_POSITION;
        for (DepTreeNode node : nodes) {
            ((ProjDepTreeNode)node).setPosition(position);
            position++;
        }
        // Set all the parent positions
        parents = new int[nodes.size()-1];
        for (int i=0; i<parents.length; i++) {
            ProjDepTreeNode parent = (ProjDepTreeNode)nodes.get(i+1).getParent();
            if (parent == null) {
                parents[i] = EMPTY_POSITION;
            } else {
                parents[i] = parent.getPosition();
            }
        }
        checkTree();
    }

    protected DepTreeNode getNodeByPosition(int position) {
        return nodes.get(position+1);
    }
    
    protected void addParentChildLinksToNodes() {
        checkTree();
        for (int i=0; i<parents.length; i++) {
            NonprojDepTreeNode child = (NonprojDepTreeNode)getNodeByPosition(i);
            NonprojDepTreeNode parent = (NonprojDepTreeNode)getNodeByPosition(parents[i]);
            child.setParent(parent);
            parent.addChild(child);
        }
    }

    protected void checkTree() {
        // Check that there is exactly one node with the WALL as its parent
        int emptyCount = countChildrenOf(parents, EMPTY_POSITION);
        if (emptyCount != 0) {
            throw new IllegalStateException("Found an empty parent cell. emptyCount=" + emptyCount);
        }
        int wallCount = countChildrenOf(parents, WallDepTreeNode.WALL_POSITION);
        if (wallCount != 1) {
            throw new IllegalStateException("There must be exactly one node with the wall as a parent. wallCount=" + wallCount);
        }
        
        // Check that there are no cyles
        if (containsCycle(parents)) {
            throw new IllegalStateException("Found cycle in parents array");
        }

        // Check for proper list lengths
        if (nodes.size()-1 != parents.length) {
            throw new IllegalStateException("Number of nodes does not equal number of parents");
        }
        
        // Check for projectivity if necessary
        if (isProjective) {
            if (!checkIsProjective(parents)) {
                throw new IllegalStateException("Found non-projective arcs in tree");
            }
        }
    }

    /**
     * Checks if a dependency tree represented as a parents array contains a cycle.
     * 
     * @param parents
     *            A parents array where parents[i] contains the index of the
     *            parent of the word at position i, with parents[i] = -1
     *            indicating that the parent of word i is the wall node.
     * @return True if the tree specified by the parents array contains a cycle,
     *         False otherwise.
     */
    public static boolean containsCycle(int[] parents) {
        for (int i=0; i<parents.length; i++) {
            int numAncestors = 0;
            int parent = parents[i];
            while(parent != WallDepTreeNode.WALL_POSITION) {
                numAncestors += 1;
                if (numAncestors > parents.length - 1) {
                    return true;
                }
                parent = parents[parent];
            }
        }
        return false;
    }

    /**
     * Checks that a dependency tree represented as a parents array is projective.
     * 
     * @param parents
     *            A parents array where parents[i] contains the index of the
     *            parent of the word at position i, with parents[i] = -1
     *            indicating that the parent of word i is the wall node.
     * @return True if the tree specified by the parents array is projective,
     *         False otherwise.
     */
    public static boolean checkIsProjective(int[] parents) {
        for (int i=0; i<parents.length; i++) {
            int pari = parents[i] == WallDepTreeNode.WALL_POSITION ? parents.length : parents[i];
            int minI = i < pari ? i : pari;
            int maxI = i > pari ? i : pari;
            for (int j=0; j<parents.length; j++) {
                if (j == i) {
                    continue;
                }
                if (minI < j && j < maxI) {
                    if (!(minI <= parents[j] && parents[j] <= maxI)) {
                        return false;
                    }
                } else {
                    if (!(parents[j] <= minI || parents[j] >= maxI)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Counts of the number of children in a dependency tree for the given
     * parent index.
     * 
     * @param parents
     *            A parents array where parents[i] contains the index of the
     *            parent of the word at position i, with parents[i] = -1
     *            indicating that the parent of word i is the wall node.
     * @return The number of entries in <code>parents</code> that equal
     *         <code>parent</code>.
     */
    public static int countChildrenOf(int[] parents, int parent) {
        int count = 0;
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == parent) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return nodes.toString();
    }

    public List<DepTreeNode> getNodes() {
        return nodes;
    }

    public Iterator<DepTreeNode> iterator() {
        return nodes.iterator();
    }
    
    /**
     * For testing only.
     * @return
     */
    public int[] getParents() {
        return parents;
    }
    
    public int getNumTokens() {
        return parents.length;
    }

    public Sentence getSentence(Alphabet<Label> alphabet) {
        return new Sentence(alphabet, this);
    }
    
}