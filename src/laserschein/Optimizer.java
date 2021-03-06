/**
 *  
 *  Laserschein. interactive ILDA output from processing and java
 *
 *  2011 by Benjamin Maus
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 *
 * @author Benjamin Maus (http://www.allesblinkt.com)
 *
 */
package laserschein;

import java.util.Vector;

/**
 * The Optimizer takes a bunch of connected points (i.e. graphics) and 
 * converts (optimizes) it for output on a laser scanner, where each 
 * sample is one fixed time slice. 
 * 
 * @author allesblinkt
 *
 */
public class Optimizer {

	private OptimizerSettings _mySettings;
	private LaserFrame _myOptimizedFrame;
	private LaserGraphic _myLaserGraphic;


	public Optimizer() {
		_mySettings = new OptimizerSettings();
		_myOptimizedFrame = new LaserFrame();
	}


	/**
	 * Does all the magic. The optimized frame can be retrieved 
	 * with {@link laserschein.Optimizer#optimizedFrame()}
	 * 
	 * @param theGraphic
	 */
	public void optimize(LaserGraphic theGraphic) {
		_myOptimizedFrame = new LaserFrame();
		_myLaserGraphic = theGraphic;

		_myLaserGraphic = eliminateDuplicates(theGraphic);

		if (_mySettings.reorderFrame) {
			_myLaserGraphic = reorderDrawing(_myLaserGraphic);
		}

		_myOptimizedFrame = createFrameWithSingleBlanks(_myLaserGraphic);

		_myOptimizedFrame = enhanceAngles(_myOptimizedFrame);
		_myOptimizedFrame = interpolatePoints(_myOptimizedFrame);
	}


	private LaserFrame createFrameWithSingleBlanks(final LaserGraphic theGraphic) {
		final LaserFrame myFrame = new LaserFrame();

		LaserPoint myPreviousPoint = null;
		LaserPoint myNextPoint = null;

		for (int i = 0; i < theGraphic.shapes().size(); i++) {

			final Vector<LaserPoint> myShape = theGraphic.shapes().get(i);
			Vector<LaserPoint> myNextShape = null;

			if (i < theGraphic.shapes().size() - 1) {
				myNextShape = theGraphic.shapes().get(i + 1);
			}

			/* Add blank */
			if (myShape.size() > 0) {
				final LaserPoint myPoint = new LaserPoint(myShape.firstElement());
				myPoint.isBlanked = true;

				myFrame.points().add(myPoint);

			}

			for (int j = 0; j < myShape.size(); j++) {

				final LaserPoint myPoint = myShape.get(j);

				if (j < myShape.size() - 1) {
					myNextPoint = myShape.get(j + 1);
				} else if (myNextShape != null && myNextShape.size() > 0) {
					myNextPoint = myNextShape.firstElement();
				} else {
					myNextPoint = null;
				}

				if (myNextPoint != null && myPreviousPoint != null) {
					myPoint.turningAngle = LaserPoint.getAngle(myPreviousPoint, myPoint, myNextPoint);
				} else {
					myPoint.turningAngle = 0;
				}

				myFrame.points().add(myPoint);

				myPreviousPoint = myPoint;

			}

			/* Add blank */
			if (myShape.size() > 0) {

				final LaserPoint myPoint = new LaserPoint(myShape.lastElement());
				myPoint.isBlanked = true;

				myFrame.points().add(myPoint);
			}
		}

		return myFrame;
	}


	private LaserGraphic eliminateDuplicates(final LaserGraphic theGraphic) {

		final LaserGraphic myNewGraphic = new LaserGraphic();

		for (final Vector<LaserPoint> myShape : theGraphic.shapes()) {

			final Vector<LaserPoint> myNewShape = new Vector<LaserPoint>();

			LaserPoint myLastPoint = null;
			for (final LaserPoint myPoint : myShape) {
				if (myLastPoint != null && myPoint.isCoincided(myLastPoint)) {
					// Omit the vertex
				} else {
					myNewShape.add(myPoint);
				}

				myLastPoint = myPoint;
			}

			myNewGraphic.shapes().add(myNewShape);
		}

		return myNewGraphic;
	}


	private LaserGraphic reorderDrawing(final LaserGraphic theGraphic) {
		return theGraphic; // TODO: implement
	}


	private LaserFrame enhanceAngles(final LaserFrame theFrame) {
		final LaserFrame myNewFrame = new LaserFrame();

		final Vector<LaserPoint> myPoints = theFrame.points();

		LaserPoint myPreviousPoint = null;
		LaserPoint myNextPoint = null;

		for (int i = 0; i < myPoints.size(); i++) {

			final LaserPoint myPoint = myPoints.get(i);

			if (i < myPoints.size() - 1) {
				myNextPoint = myPoints.get(i + 1);
			} else {
				myNextPoint = null;
			}

			
			/* Blanks */
			if (myPoint.isBlanked) {
				
				int myNumber = 0;
				
		
				
				if (myPreviousPoint == null || myPreviousPoint.isBlanked) {
					myNumber = _mySettings.extraBlankPointsStart;

				}
				
				if (myNextPoint == null || myNextPoint.isBlanked) {
					myNumber = _mySettings.extraBlankPointsEnd;

				}
				
				
				for (int j = 0; j < myNumber; j++) {
					final LaserPoint myBlankPoint = new LaserPoint(myPoint);
					myNewFrame.points().add(myBlankPoint);
				}
			}

			/* Add extra points (angle independent) */
			if (myPoint.isCorner && !_mySettings.analyzeCornerAngles ) {

				int myNumber = _mySettings.extraCornerPoints;

				if (myPreviousPoint == null || myPreviousPoint.isBlanked) {
					myNumber = _mySettings.extraCornerPointsStart;

				}
				
				
				if (myNextPoint == null || myNextPoint.isBlanked) {
					myNumber = _mySettings.extraCornerPointsEnd;
				}

				for (int j = 0; j < myNumber; j++) {
					myNewFrame.points().add(new LaserPoint(myPoint));
				}

			}

			/* Add extra points (angle dependent) */
			if (myPoint.isCorner && _mySettings.analyzeCornerAngles) {
				float myAngle = myPoint.turningAngle;

				int myNumber = _mySettings.extraCornerPoints;
				int myExtraPointCount = (int) ((1f - (myAngle / (float) Math.PI)) * _mySettings.extraCornerPointsAngleDependent);
				

				if (myPreviousPoint == null || myPreviousPoint.isBlanked) {
					myNumber = _mySettings.extraCornerPointsStart;
				}
								
				if (myNextPoint == null || myNextPoint.isBlanked) {
					myNumber = _mySettings.extraCornerPointsEnd;
				}
				
				myNumber += myExtraPointCount;

				

				for (int j = 0; j < myNumber; j++) {
					myNewFrame.points().add(new LaserPoint(myPoint));
				}

			}
			
			
			/* Add extra points for smoot shapes at the end */
			if (!myPoint.isCorner ) {
				float myAngle = myPoint.turningAngle;

				int myNumber = 0;
				
				int myExtraPointCount = 0;
				
				if (myPreviousPoint == null || myPreviousPoint.isBlanked) {
					myNumber = _mySettings.extraCornerPointsStart;
					myExtraPointCount = (int) ((1f - (myAngle / (float) Math.PI)) * _mySettings.extraCornerPointsAngleDependent);

				}
								
				if (myNextPoint == null || myNextPoint.isBlanked) {
					myNumber = _mySettings.extraCornerPointsEnd;
					myExtraPointCount = (int) ((1f - (myAngle / (float) Math.PI)) * _mySettings.extraCornerPointsAngleDependent);
				}
				
				myNumber += myExtraPointCount;

				for (int j = 0; j < myNumber; j++) {
					myNewFrame.points().add(new LaserPoint(myPoint));
				}

			}
			
			
			myNewFrame.points().add(myPoint);

			myPreviousPoint = myPoint;
		}

		return myNewFrame;
	}


	private LaserFrame interpolatePoints(final LaserFrame theFrame) {
		final LaserFrame myNewFrame = new LaserFrame();

		final Vector<LaserPoint> myPoints = theFrame.points();

		LaserPoint myNextPoint = null;
		for (int i = 0; i < myPoints.size(); i++) {
			final LaserPoint myPoint = myPoints.get(i);

			if (i < myPoints.size() - 1) {
				myNextPoint = myPoints.get(i + 1);
			} else {
				myNextPoint = null;
			}

			myNewFrame.points().add(myPoint);

			if (myNextPoint != null &&
					!myNextPoint.isBlanked &&
					myPoint.isCorner &&
					myNextPoint.isCorner &&
					myPoint.distance(myNextPoint) > _mySettings.maxTravel) {
				float myDistance = myPoint.distance(myNextPoint);

				int mySteps = (int) (myDistance / _mySettings.maxTravel);
				float myIncrement = 1f / (float) (mySteps + 1);

				for (int j = 0; j < mySteps; j++) {
					float myProgress = myIncrement + myIncrement * j;
					LaserPoint myBetweenPoint = myPoint.getPointBetween(myNextPoint, myProgress);

					myNewFrame.points().add(myBetweenPoint);
				}
			}
		}

		return myNewFrame;
	}


	/**
	 * @return the frame suited for display on a laser system
	 */
	public LaserFrame optimizedFrame() {
		return _myOptimizedFrame;
	}


	public OptimizerSettings settings() {
		return _mySettings;
	}


	public void setSettingsRef(OptimizerSettings theSettings) {
		_mySettings = theSettings;
	}

}
