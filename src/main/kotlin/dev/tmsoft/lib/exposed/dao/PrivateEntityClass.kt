package dev.tmsoft.lib.exposed.dao

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.OptionalBackReference
import org.jetbrains.exposed.dao.OptionalReferrers
import org.jetbrains.exposed.dao.Referrers
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import kotlin.properties.ReadOnlyProperty

private typealias OtherPrivateEntityClass<ID, T> = PrivateEntityClass<ID, T>

open class PrivateEntityClass<ID : Comparable<ID>, out T : Entity<ID>>(private val base: EntityClass<ID, T>) {
    open fun new(init: T.() -> Unit) = base.new(null, init)

    open fun new(id: ID?, init: T.() -> Unit) = base.new(id, init)

    fun get(id: EntityID<ID>): T = base[id]

    operator fun get(id: ID): T = base[id]

    infix fun <REF : Comparable<REF>> referencedOn(column: Column<REF>) = base.referencedOn(column)

    infix fun <REF : Comparable<REF>> optionalReferencedOn(column: Column<REF?>) = base.optionalReferencedOn(column)

    infix fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.backReferencedOn(
        column: Column<REF>
    ): ReadOnlyProperty<Entity<ID>, Target> = with(this@PrivateEntityClass.base) {
        this@backReferencedOn.base.backReferencedOn(column)
    }

    @JvmName("backReferencedOnOpt")
    infix fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.backReferencedOn(
        column: Column<REF?>
    ): ReadOnlyProperty<Entity<ID>, Target> = with(this@PrivateEntityClass.base) {
        this@backReferencedOn.base.backReferencedOn(column)
    }

    infix fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.optionalBackReferencedOn(
        column: Column<REF>
    ): OptionalBackReference<TargetID, Target, ID, Entity<ID>, REF> = with(this@PrivateEntityClass.base) {
        this@optionalBackReferencedOn.base.optionalBackReferencedOn(column)
    }

    @JvmName("optionalBackReferencedOnOpt")
    infix fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.optionalBackReferencedOn(
        column: Column<REF?>
    ): OptionalBackReference<TargetID, Target, ID, Entity<ID>, REF> = with(this@PrivateEntityClass.base) {
        this@optionalBackReferencedOn.base.optionalBackReferencedOn(column)
    }

    infix fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.referrersOn(
        column: Column<REF>
    ): Referrers<ID, Entity<ID>, TargetID, Target, REF> = with(this@PrivateEntityClass.base) {
        this@referrersOn.base.referrersOn(column)
    }

    fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.referrersOn(
        column: Column<REF>,
        cache: Boolean
    ): Referrers<ID, Entity<ID>, TargetID, Target, REF> = with(this@PrivateEntityClass.base) {
        this@referrersOn.base.referrersOn(column, cache)
    }

    infix fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.optionalReferrersOn(
        column: Column<REF?>
    ): OptionalReferrers<ID, Entity<ID>, TargetID, Target, REF> = with(this@PrivateEntityClass.base) {
        this@optionalReferrersOn.base.optionalReferrersOn(column)
    }

    fun <
            TargetID : Comparable<TargetID>,
            Target : Entity<TargetID>,
            REF : Comparable<REF>
            > OtherPrivateEntityClass<TargetID, Target>.optionalReferrersOn(
        column: Column<REF?>,
        cache: Boolean = false
    ): OptionalReferrers<ID, Entity<ID>, TargetID, Target, REF> = with(this@PrivateEntityClass.base) {
        this@optionalReferrersOn.base.optionalReferrersOn(column, cache)
    }
}
