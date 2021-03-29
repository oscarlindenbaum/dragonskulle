/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.materials.VertexHighlightMaterial;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.renderer.*;
import org.dragonskulle.renderer.TextureMapping.TextureFiltering;
import org.dragonskulle.renderer.TextureMapping.TextureWrapping;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.IColouredMaterial;

/**
 * @author Leela Muppala
 *     <p>Creates each HexagonTile with their 3 coordinates. This stores information about the axial
 *     coordinates of each tile.
 */
@Log
@Accessors(prefix = "m")
public class HexagonTile {

    // A variable which changes the colour of the hex tiles to make them easier to see
    private static final boolean DEBUG = false;

    /** Describes a template for land hex tile */
    static final GameObject LAND_TILE =
            new GameObject(
                    "land",
                    (go) -> {
                        Mesh mesh = Mesh.HEXAGON;
                        SampledTexture texture =
                                new SampledTexture(
                                        "map/grass.png",
                                        new TextureMapping(
                                                TextureFiltering.LINEAR, TextureWrapping.REPEAT));

                        IColouredMaterial mat = new VertexHighlightMaterial(texture);
                        mat.getColour().set(0f, 1f, 0f, 1f);
                        go.addComponent(new Renderable(mesh, mat));

                        if (DEBUG) {
                            Reference<Renderable> hexRenderer = go.getComponent(Renderable.class);
                            VertexHighlightMaterial hexMaterial =
                                    hexRenderer.get().getMaterial(VertexHighlightMaterial.class);
                            hexMaterial.setDistancePow(20f);
                            hexMaterial.getTexColour().set(0.1f, 0.1f, 0.1f, 1.f);
                        }
                    });

    /** This is the axial storage system for each tile */
    @Getter private final int mQ;

    @Getter private final int mR;

    @Getter private final int mS;

    /**
     * Associated game object.
     *
     * <p>This is specifically package-only, since the game does not need to know about underlying
     * tile objects.
     */
    @Getter(AccessLevel.PACKAGE)
    private final GameObject mGameObject;

    /** Building that is on the tile */
    @Getter @Setter private Building mBuilding;

    /** A reference to the building that claims the tile, or {@code null}. */
    private Reference<Building> mClaimedBy = new Reference<Building>(null);

    /**
     * Constructor that creates the HexagonTile with a test to see if all the coordinates add up to
     * 0.
     *
     * @param q The first coordinate.
     * @param r The second coordinate.
     * @param s The third coordinate.
     */
    HexagonTile(int q, int r, int s) {
        this.mQ = q;
        this.mR = r;
        this.mS = s;
        mGameObject = GameObject.instantiate(LAND_TILE, new TransformHex(mQ, mR));
        if (q + r + s != 0) {
            log.warning("The coordinates do not add up to 0");
        }
    }

    /** The length of the tile from the origin */
    public int length() {
        return (int) ((Math.abs(mQ) + Math.abs(mR) + Math.abs(mS)) / 2);
    }

    public int distTo(int q, int r) {
        int s = -q - r;

        return (int) ((Math.abs(q - mQ) + Math.abs(r - mR) + Math.abs(s - mS)) / 2);
    }

    @Override
    public String toString() {
        return Arrays.toString(new int[] {this.mQ, this.mR, this.mS});
    }
    
    /**
     * Set which {@link Building} claims the tile. Do not claim the tile if another building already claimed it.
     * 
     * @param building The building which claimed the tile. 
     * @return {@code true} if the claim was successful, otherwise {@code false} if the tile is already claimed.
     */
    public boolean setClaimedBy(Building building) {
		if(isClaimed()) return false;
    	
		mClaimedBy = building.getReference(Building.class);
		return true;
    }
    
    /**
     * Whether the tile is claimed by a building.
     * 
     * @return Whether the tile is claimed by a building.
     */
    public boolean isClaimed() {
    	if(mClaimedBy == null || mClaimedBy.isValid() == false) return false;
    	return true;
    }
    
    /**
     * Get the {@link Player} who has claimed this tile (either because there is a building on it or because it is adjacent to a building).
     * 
     * @return The Player who has claimed this tile, or {@code null} if no Player claims it.
     */
    public Player getClaimant() {
    	if(!isClaimed()) {
    		return null;    		
    	}
    	return mClaimedBy.get().getOwner();
    }
}
