package gamma.engine.core.components;

import gamma.engine.core.annotations.EditorRange;
import gamma.engine.core.annotations.EditorVariable;
import gamma.engine.core.physics.Collision3D;
import vecmatlib.vector.Vec3f;

/**
 * Controls an entity's movement through kinematic equations.
 * The entity needs a {@link Transform3D} component to move.
 *
 * @see CollisionObject3D
 *
 * @author Nico
 */
public class KinematicBody3D extends CollisionObject3D {

	/** The entity's current velocity */
	@EditorVariable("Velocity")
	@EditorRange
	public Vec3f velocity = Vec3f.Zero();
	/** The entity's current acceleration */
	@EditorVariable("Acceleration")
	@EditorRange
	public Vec3f acceleration = new Vec3f(0.0f, -9.81f, 0.0f);

	@Override
	protected void onUpdate(float delta) {
		super.onUpdate(delta);
		Vec3f velocityDelta = this.acceleration.multipliedBy(delta);
		this.velocity = this.velocity.plus(velocityDelta);
		this.getComponent(Transform3D.class).ifPresent(transform -> {
			Vec3f movement = this.velocity.multipliedBy(delta);
			this.moveAndCollide(movement);
		});
	}

	@Override
	protected void onCollision(Collision3D collision) {
		this.getComponent(Transform3D.class).ifPresent(transform -> {
			transform.position = transform.position.plus(collision.normal().multipliedBy(collision.depth()));
			this.velocity = this.velocity.slide(collision.normal());
		});
	}
}