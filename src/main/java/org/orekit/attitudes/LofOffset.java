/* Copyright 2002-2014 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.attitudes;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * Attitude law defined by fixed Roll, Pitch and Yaw angles (in any order)
 * with respect to a local orbital frame.

 * <p>
 * The attitude provider is defined as a rotation offset from some local orbital frame.
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class LofOffset implements AttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -713570668596014285L;

    /** Type of Local Orbital Frame. */
    private LOFType type;

    /** Rotation from local orbital frame.  */
    private final Rotation offset;

    /** Inertial frame with respect to which orbit should be computed. */
    private final Frame inertialFrame;

    /** Create a LOF-aligned attitude.
     * <p>
     * Calling this constructor is equivalent to call
     * {@code LofOffset(intertialFrame, LOFType.VVLH)}
     * </p>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @exception OrekitException if inertialFrame is not a pseudo-inertial frame
     * @deprecated as of 6.0 replaced by {@link #LofOffset(Frame, LOFType)}
     */
    @Deprecated
    public LofOffset(final Frame inertialFrame) throws OrekitException {
        this(inertialFrame, LOFType.VVLH);
    }

    /** Creates new instance.
     * <p>
     * Calling this constructor is equivalent to call
     * {@code LofOffset(intertialFrame, LOFType.VVLH, order, alpha1, alpha2, alpha3)}
     * </p>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     * @exception OrekitException if inertialFrame is not a pseudo-inertial frame
     * @deprecated as of 6.0 replaced by {@link #LofOffset(Frame, LOFType, RotationOrder, double, double, double)}
     */
    @Deprecated
    public LofOffset(final Frame inertialFrame,
                     final RotationOrder order, final double alpha1,
                     final double alpha2, final double alpha3) throws OrekitException {
        this(inertialFrame, LOFType.VVLH, order, alpha1, alpha2, alpha3);
    }

    /** Create a LOF-aligned attitude.
     * <p>
     * Calling this constructor is equivalent to call
     * {@code LofOffset(inertialFrame, LOFType, RotationOrder.XYZ, 0, 0, 0)}
     * </p>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param type type of Local Orbital Frame
     * @exception OrekitException if inertialFrame is not a pseudo-inertial frame
     */
    public LofOffset(final Frame inertialFrame, final LOFType type) throws OrekitException {
        this(inertialFrame, type, RotationOrder.XYZ, 0, 0, 0);
    }

    /** Creates new instance.
     * <p>
     * An important thing to note is that the rotation order and angles signs used here
     * are compliant with an <em>attitude</em> definition, i.e. they correspond to
     * a frame that rotate in a field of fixed vectors. The underlying definitions used
     * in commons-math {@link org.apache.commons.math3.geometry.euclidean.threed.Rotation#Rotation(RotationOrder,
     * double, double, double) Rotation(RotationOrder, double, double, double)} use
     * <em>reversed</em> definition, i.e. they correspond to a vectors field rotating
     * with respect to a fixed frame. So to retrieve the angles provided here from the
     * commons-math underlying rotation, one has to <em>revert</em> the rotation, as in
     * the following code snippet:
     * </p>
     * <pre>
     *   LofOffset law          = new LofOffset(inertial, lofType, order, alpha1, alpha2, alpha3);
     *   Rotation  offsetAtt    = law.getAttitude(orbit).getRotation();
     *   Rotation  alignedAtt   = new LofOffset(inertial, lofType).getAttitude(orbit).getRotation();
     *   Rotation  offsetProper = offsetAtt.applyTo(alignedAtt.revert());
     *
     *   // note the call to revert in the following statement
     *   double[] angles = offsetProper.revert().getAngles(order);
     *
     *   System.out.println(alpha1 + " == " + angles[0]);
     *   System.out.println(alpha2 + " == " + angles[1]);
     *   System.out.println(alpha3 + " == " + angles[2]);
     * </pre>
     * @param inertialFrame inertial frame with respect to which orbit should be computed
     * @param type type of Local Orbital Frame
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     * @exception OrekitException if inertialFrame is not a pseudo-inertial frame
     */
    public LofOffset(final Frame inertialFrame, final LOFType type,
                     final RotationOrder order, final double alpha1,
                     final double alpha2, final double alpha3) throws OrekitException {
        this.type = type;
        this.offset = new Rotation(order, alpha1, alpha2, alpha3).revert();
        if (!inertialFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS,
                                      inertialFrame.getName());
        }
        this.inertialFrame = inertialFrame;
    }


    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // construction of the local orbital frame, using PV from inertial frame
        final PVCoordinates pv = pvProv.getPVCoordinates(date, inertialFrame);
        final Transform inertialToLof = type.transformFromInertial(date, pv);

        // take into account the specified start frame (which may not be an inertial one)
        final Transform frameToInertial = frame.getTransformTo(inertialFrame, date);
        final Transform frameToLof = new Transform(date, frameToInertial, inertialToLof);

        // compose with offset rotation
        return new Attitude(date, frame,
                            offset.applyTo(frameToLof.getRotation()),
                            offset.applyTo(frameToLof.getRotationRate()));

    }

}
