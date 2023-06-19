package io.github.lwjre.engine.nodes;

import io.github.hexagonnico.vecmatlib.matrix.Mat4f;
import io.github.hexagonnico.vecmatlib.vector.Vec3f;
import io.github.hexagonnico.vecmatlib.vector.Vec4f;
import io.github.lwjre.engine.annotations.EditorRange;
import io.github.lwjre.engine.annotations.EditorVariable;

import java.util.HashSet;
import java.util.List;

/**
 * Node that represents a realistic 3D rigid body.
 *
 * @author Nico
 */
public class RigidBody3D extends DynamicBody3D {

	/**
	 * The object's angular velocity in radians per second.
	 */
	@EditorVariable(name = "Angular velocity")
	@EditorRange
	public Vec3f angularVelocity = new Vec3f(0.0f, 0.0f, 0.0f);

	/**
	 * The object's angular acceleration in radians per second squared.
	 */
	@EditorVariable(name = "Angular acceleration")
	@EditorRange
	public Vec3f angularAcceleration = new Vec3f(0.0f, 0.0f, 0.0f);

	private Vec3f inertia = new Vec3f(0.0f, 0.0f, 0.0f);

	/** Current torque */
	private Vec3f torque = new Vec3f(0.0f, 0.0f, 0.0f);

	@Override
	protected void onEnter() {
		Vec3f shape = this.boundingBox.multiply(this.globalScale());
		this.inertia = new Vec3f(
				1.0f / 12.0f * this.mass * (shape.y() * shape.y() + shape.z() * shape.z()),
				1.0f / 12.0f * this.mass * (shape.x() * shape.x() + shape.z() * shape.z()),
				1.0f / 12.0f * this.mass * (shape.x() * shape.x() + shape.y() * shape.y())
		);
		super.onEnter();
	}

	@Override
	protected void onUpdate(float delta) {
		this.angularAcceleration = this.angularAcceleration.plus(this.torque.divide(this.inertia));
		this.angularVelocity = this.angularVelocity.plus(this.angularAcceleration.multipliedBy(delta));
		this.rotation = this.rotation.plus(this.angularVelocity.multipliedBy(delta));
		super.onUpdate(delta);
	}

	@Override
	protected void onCollision(CollisionObject3D collider, Vec3f normal, float depth) {
		this.position = this.position.plus(normal.multipliedBy(depth));
		HashSet<Vec3f> intersectionPoints = this.intersectionPoints(collider);
		float massB = collider instanceof RigidBody3D ? ((RigidBody3D) collider).mass : Float.POSITIVE_INFINITY;
		Vec3f inertiaB = collider instanceof RigidBody3D ? ((RigidBody3D) collider).inertia : new Vec3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		Vec3f colliderVelocity = Vec3f.Zero();
		Vec3f colliderAngularVelocity = Vec3f.Zero();
		Vec3f relativeLinearVelocity = colliderVelocity.minus(this.velocity);
		Vec3f velocityDelta = Vec3f.Zero();
		Vec3f angularVelocityDeltaA = Vec3f.Zero();
		Vec3f angularVelocityDeltaB = Vec3f.Zero();
		for(Vec3f point : intersectionPoints) {
			Vec3f radiusA = point.minus(this.globalPosition());
			Vec3f radiusB = point.minus(collider.globalPosition());
			Vec3f relativeAngularVelocity = colliderAngularVelocity.cross(radiusB).minus(this.angularVelocity.cross(radiusA));
			Vec3f relativeVelocity = relativeLinearVelocity.plus(relativeAngularVelocity);
			float impulseMagnitude = -relativeVelocity.multipliedBy(1 + this.restitution).dot(normal) / (1.0f / this.mass + 1.0f / massB + radiusA.cross(normal).divide(this.inertia).cross(radiusA).plus(radiusB.cross(normal).divide(inertiaB).cross(radiusB)).dot(normal));
			Vec3f impulse = normal.multipliedBy(impulseMagnitude / intersectionPoints.size());
			velocityDelta = velocityDelta.plus(impulse.dividedBy(this.mass));
			angularVelocityDeltaA = angularVelocityDeltaA.plus(radiusA.cross(impulse).divide(this.inertia));
			angularVelocityDeltaB = angularVelocityDeltaB.plus(radiusB.cross(impulse).divide(inertiaB));
		}
		this.velocity = this.velocity.minus(velocityDelta);
		this.angularVelocity = this.angularVelocity.plus(angularVelocityDeltaA);
		if(collider instanceof RigidBody3D rigidBody) {
			rigidBody.velocity = rigidBody.velocity.plus(velocityDelta);
			rigidBody.angularVelocity = rigidBody.angularVelocity.minus(angularVelocityDeltaB);
		}
	}

	/**
	 * Finds the intersection points between this collider and the given one.
	 *
	 * @param collider The other collider
	 * @return A {@link HashSet} containing the points of intersection
	 */
	private HashSet<Vec3f> intersectionPoints(CollisionObject3D collider) {
		HashSet<Vec3f> intersectionPoints = new HashSet<>();
		intersectionPoints(collider, this.getEdges(), intersectionPoints);
		intersectionPoints(this, collider.getEdges(), intersectionPoints);
		return intersectionPoints;
	}

	/**
	 * Finds the intersection points between the given collider and the list of edges.
	 * Intersection points are computed as line-plane intersections.
	 *
	 * @param collider The collider to check
	 * @param edges The edges of the other collider
	 * @param intersectionPoints Result list
	 */
	private static void intersectionPoints(CollisionObject3D collider, List<Vec3f> edges, HashSet<Vec3f> intersectionPoints) {
		Mat4f transform = collider.globalTransformation();
		Vec3f colliderPosition = transform.multiply(new Vec4f(collider.position, 1.0f)).xyz();
		Mat4f colliderRotation = collider.globalRotation();
		Vec3f boundingBox = collider.boundingBox.multiply(collider.globalScale());
		Vec3f[] normals = new Vec3f[] {
				colliderRotation.col0().xyz(),
				colliderRotation.col1().xyz(),
				colliderRotation.col2().xyz()
		};
		Vec3f[] points = new Vec3f[] {
				transform.multiply(new Vec4f(collider.boundingBox.dividedBy(-2.0f), 1.0f)).xyz(),
				transform.multiply(new Vec4f(collider.boundingBox.dividedBy(2.0f), 1.0f)).xyz()
		};
		for(Vec3f normal : normals) {
			for(Vec3f point : points) {
				float d = normal.dot(point);
				for(int i = 0; i < edges.size(); i += 2) {
					Vec3f direction = edges.get(i + 1).minus(edges.get(i));
					if(Math.abs(normal.dot(direction)) > 0.0f) {
						float t = (d - normal.dot(edges.get(i))) / normal.dot(direction);
						Vec3f candidate = edges.get(i).plus(direction.multipliedBy(t));
						if(t >= -0.00001f && t <= 1.00001f && isPointValid(candidate, colliderPosition, colliderRotation, boundingBox)) {
							intersectionPoints.add(candidate);
						}
					}
				}
			}
		}
	}

	/**
	 * Checks if the given point is inside the collider's bounding box.
	 * Intersection points are computed as line-plane intersections.
	 * This method checks if the computed point is within the collider's bounds.
	 *
	 * @param point The point to check
	 * @param colliderPosition The collider's global position
	 * @param colliderRotation The collider's rotation matrix
	 * @param boundingBox The size of the collider's bounding box
	 * @return True if the point is inside the collider's bounding box, otherwise false
	 */
	private static boolean isPointValid(Vec3f point, Vec3f colliderPosition, Mat4f colliderRotation, Vec3f boundingBox) {
		Vec3f v = point.minus(colliderPosition);
		float px = Math.abs(v.dot(colliderRotation.col0().xyz()));
		float py = Math.abs(v.dot(colliderRotation.col1().xyz()));
		float pz = Math.abs(v.dot(colliderRotation.col2().xyz()));
		return 2 * px <= boundingBox.x() && 2 * py <= boundingBox.y() && 2 * pz <= boundingBox.z();
	}

	/**
	 * Gets this object's resulting torque.
	 *
	 * @return This object's resulting torque
	 */
	public final Vec3f torque() {
		return this.torque;
	}

	/**
	 * Applies a positioned force to this object.
	 *
	 * @param force The force to apply
	 * @param radius The vector from the object's center of mass to the point where the force is applied
	 */
	public void applyForce(Vec3f force, Vec3f radius) {
		super.applyForce(force);
		this.torque = this.torque.plus(radius.cross(force));
	}

	/**
	 * Applies a positioned force to this object.
	 *
	 * @param fx The force to apply on the x axis
	 * @param fy The force to apply on the y axis
	 * @param fz The force to apply on the z axis
	 * @param radius The vector from the object's center of mass to the point where the force is applied
	 */
	public final void applyForce(float fx, float fy, float fz, Vec3f radius) {
		this.applyForce(new Vec3f(fx, fy, fz), radius);
	}

	/**
	 * Applies a positioned force to this object.
	 *
	 * @param force The force to apply
	 * @param rx Distance of the point where the force is applied from the object's center of mass on the x axis
	 * @param ry Distance of the point where the force is applied from the object's center of mass on the y axis
	 * @param rz Distance of the point where the force is applied from the object's center of mass on the z axis
	 */
	public final void applyForce(Vec3f force, float rx, float ry, float rz) {
		this.applyForce(force, new Vec3f(rx, ry, rz));
	}

	/**
	 * Applies a positioned force to this object.
	 *
	 * @param fx The force to apply on the x axis
	 * @param fy The force to apply on the y axis
	 * @param fz The force to apply on the z axis
	 * @param rx Distance of the point where the force is applied from the object's center of mass on the x axis
	 * @param ry Distance of the point where the force is applied from the object's center of mass on the y axis
	 * @param rz Distance of the point where the force is applied from the object's center of mass on the z axis
	 */
	public final void applyForce(float fx, float fy, float fz, float rx, float ry, float rz) {
		this.applyForce(new Vec3f(fx, fy, fz), new Vec3f(rx, ry, rz));
	}

	/**
	 * Applies a positioned impulse to this object, a time-independent force that simulates a one-time impact.
	 *
	 * @param impulse The impulse to apply
	 * @param radius The vector from the object's center of mass to the point where the impulse is applied
	 */
	public void applyImpulse(Vec3f impulse, Vec3f radius) {
		super.applyImpulse(impulse);
		this.angularVelocity = this.angularVelocity.plus(radius.cross(impulse).divide(this.inertia));
	}

	/**
	 * Applies a positioned impulse to this object, a time-independent force that simulates a one-time impact.
	 *
	 * @param ix The impulse to apply on the x axis
	 * @param iy The impulse to apply on the y axis
	 * @param iz The impulse to apply on the z axis
	 * @param radius The vector from the object's center of mass to the point where the impulse is applied
	 */
	public final void applyImpulse(float ix, float iy, float iz, Vec3f radius) {
		this.applyImpulse(new Vec3f(ix, iy, iz), radius);
	}

	/**
	 * Applies a positioned impulse to this object, a time-independent force that simulates a one-time impact.
	 *
	 * @param impulse The impulse to apply
	 * @param rx Distance of the point where the impulse is applied from the object's center of mass on the x axis
	 * @param ry Distance of the point where the impulse is applied from the object's center of mass on the y axis
	 * @param rz Distance of the point where the impulse is applied from the object's center of mass on the z axis
	 */
	public final void applyImpulse(Vec3f impulse, float rx, float ry, float rz) {
		this.applyImpulse(impulse, new Vec3f(rx, ry, rz));
	}

	/**
	 * Applies a positioned impulse to this object, a time-independent force that simulates a one-time impact.
	 *
	 * @param ix The impulse to apply on the x axis
	 * @param iy The impulse to apply on the y axis
	 * @param iz The impulse to apply on the z axis
	 * @param rx Distance of the point where the impulse is applied from the object's center of mass on the x axis
	 * @param ry Distance of the point where the impulse is applied from the object's center of mass on the y axis
	 * @param rz Distance of the point where the impulse is applied from the object's center of mass on the z axis
	 */
	public final void applyImpulse(float ix, float iy, float iz, float rx, float ry, float rz) {
		this.applyImpulse(new Vec3f(ix, iy, iz), new Vec3f(rx, ry, rz));
	}

	/**
	 * Applies a rotation force to this object.
	 *
	 * @param torque The rotational force to apply
	 */
	public void applyTorque(Vec3f torque) {
		this.torque = this.torque.plus(torque);
	}

	/**
	 * Applies a rotation force to this object.
	 *
	 * @param x The rotational force to apply on the x axis
	 * @param y The rotational force to apply on the y axis
	 * @param z The rotational force to apply on the z axis
	 */
	public final void applyTorque(float x, float y, float z) {
		this.applyTorque(new Vec3f(x, y, z));
	}
}
