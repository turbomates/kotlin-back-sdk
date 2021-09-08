package dev.tmsoft.lib.query

import dev.tmsoft.lib.Config.h2DatabaseUrl
import dev.tmsoft.lib.Config.h2Driver
import dev.tmsoft.lib.Config.h2Password
import dev.tmsoft.lib.Config.h2User
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EntityBuilderTest {

    private val database : Database

    init {
        database = Database.connect(h2DatabaseUrl, driver = h2Driver, user = h2User, password = h2Password)
    }

    private fun transactionalOperation (operation: () -> Unit) {
        transaction(database) {
            SchemaUtils.create(Countries, CountryLanguages, Presidents)
            operation()
        }
    }

    @Test
    fun foldSimpleEntityBuild() {
        transactionalOperation {
            SchemaUtils.create(Countries, CountryLanguages, Presidents)
            val countries = expectedData()
            countries.forEach{ country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
          assertEquals(countries, getCountries())
        }
    }

    @Test
    fun foldForNotExistingRootTable() {
        transactionalOperation {
            val countries = expectedData2()
            countries.forEach{ country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertFailsWith<IllegalArgumentException>(
                message = "The corresponding root table is not present in the resulting query",
                block = {
                    getExtraneousEntities()
                }
            )
        }
    }

    @Test
    fun foldBuildRootOnly() {
        transactionalOperation {
            SchemaUtils.create(Countries, CountryLanguages, Presidents)
            val countries = expectedData()
            countries.forEach{ country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertEquals(getCountriesData(), getCountriesAsRoots())
        }
    }

    @Test
    fun foldComplexEntityBuild() {
        transactionalOperation {
            SchemaUtils.create(Countries, CountryLanguages, Presidents)
            val countries = expectedData2()
            countries.forEach { country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertEquals(countries.flatMap { it.presidents }, getPresidents())
        }
    }

    private fun expectedData(): List<Country> {
        val (belgium, belarus) = getCountriesData()
        val belarusPresident = getBelarusPresidents(belarus)
        val belgiumLanguages = getBelgiumLanguages(belgium)
        val belarusLanguages = getBelarusLanguages(belarus)

        belarus.presidents = belarusPresident
        belgium.countryLanguages = belgiumLanguages
        belarus.countryLanguages = belarusLanguages

        return listOf(belgium, belarus)
    }

    private fun expectedData2(): List<Country> {
        val (belgium, belarus) = getCountriesData()

        belgium.countryLanguages = getBelgiumLanguages(belgium)
        belarus.countryLanguages = getBelarusLanguages(belarus)
        belarus.presidents = getBelarusPresidents(belarus)

        return listOf(belgium, belarus)
    }

    private fun getCountriesData() : List<Country> {
        return listOf(Country("2", "Belgium", "30.689"), Country("1", "Belarus", "207.6"))
    }

    private fun getBelarusPresidents(country : Country) : MutableList<President> {
        return mutableListOf(President("1", "Tihanovskaya", "2020 - till now", country.copy()),
              President("2", "Lukashenko", "1994 - 2020", country.copy()))
    }

    private fun getBelgiumLanguages(belgium : Country) : MutableList<CountryLanguage> {
        return mutableListOf(CountryLanguage("3", "Dutch", belgium.copy()),
        CountryLanguage("4", "French", belgium.copy()), CountryLanguage("5", "German", belgium.copy()))
    }

    private fun getBelarusLanguages(belarus : Country) : MutableList<CountryLanguage> {
        return mutableListOf(CountryLanguage("1", "Russian", belarus.copy()),
            CountryLanguage("2", "Belorussian", belarus.copy()))
    }

    private fun getCountries(): List<Country> {
        val dependencyMapper: MutableMap<IdTable<*>, ResultRow.(Country) -> Unit> = mutableMapOf()
        dependencyMapper[CountryLanguages] = { country -> country.countryLanguages.add(toCountryLanguage()) }
        dependencyMapper[Presidents] = { country -> country.presidents.add(toPresident()) }
        return Countries
            .join(Presidents, JoinType.LEFT, Presidents.country, Countries.id)
            .join(CountryLanguages, JoinType.LEFT, CountryLanguages.country, Countries.id)
            .selectAll()
            .fold(Countries, { toCountry() }, dependencyMapper)
    }

    private fun getPresidents() : List<President> {
        val dependencyMapper: MutableMap<IdTable<*>, ResultRow.(President) -> Unit> = mutableMapOf()
        dependencyMapper[Countries] = { president -> president.country = toCountry() }
        dependencyMapper[CountryLanguages] = { president -> president.country.countryLanguages.add(toCountryLanguage()) }
        return Countries
            .join(Presidents, JoinType.LEFT, Presidents.country, Countries.id)
            .join(CountryLanguages, JoinType.LEFT, CountryLanguages.country, Countries.id)
            .selectAll()
            .fold(Presidents, { toPresident() }, dependencyMapper)
    }

    private fun getCountriesAsRoots() : List<Country> {
        return Countries
            .join(Presidents, JoinType.LEFT, Presidents.country, Countries.id)
            .join(CountryLanguages, JoinType.LEFT, CountryLanguages.country, Countries.id)
            .selectAll()
            .fold(Countries, { toCountry() }, null)
    }

    private fun getExtraneousEntities() : List<ExtraneousEntity> {
        return Countries
            .join(Presidents, JoinType.LEFT, Presidents.country, Countries.id)
            .join(CountryLanguages, JoinType.LEFT, CountryLanguages.country, Countries.id)
            .selectAll()
            .fold(ExtraneousTable, { toExtraneousEntity() }, null)
    }

    private fun ResultRow.toPresident(): President {
        return President(
            this[Presidents.id].value,
            this[Presidents.name],
            this[Presidents.governmentYears],
            Country(this[Presidents.country], this[Countries.name], this[Countries.area])
        )
    }

    private fun ResultRow.toCountryLanguage(): CountryLanguage {
        return CountryLanguage(
            this[CountryLanguages.id].value,
            this[CountryLanguages.name],
            Country(this[CountryLanguages.country], this[Countries.name], this[Countries.area])
        )
    }

    private fun ResultRow.toCountry(): Country {
        return Country(
            this[Countries.id].value,
            this[Countries.name],
            this[Countries.area]
        )
    }

    private fun ResultRow.toExtraneousEntity(): ExtraneousEntity {
        return ExtraneousEntity(
            this[Countries.id].value
        )
    }

    data class Country(
        val id: String,
        val name: String,
        val area: String,
        var countryLanguages: MutableList<CountryLanguage> = mutableListOf(),
        var presidents: MutableList<President> = mutableListOf()
    )

    object Countries : IdTable<String>("countries") {
        override val id: Column<EntityID<String>> = varchar("id", 20).entityId()
        val name = varchar("type", 20)
        val area = varchar("area", 10)
        override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
    }

    data class ExtraneousEntity(val id: String)

    object ExtraneousTable : IdTable<String>("extraneous_table") {
        override val id: Column<EntityID<String>> = varchar("id", 20).entityId()
        override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
    }

    data class CountryLanguage(val id: String, val name: String, var country: Country)

    object CountryLanguages : IdTable<String>("country_languages") {
        override val id: Column<EntityID<String>> = varchar("id", 20).entityId()
        val name = varchar("name", 20)
        val country = varchar("country", 20)
        override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
    }

    data class President(val id: String, val name: String, val governmentYears: String, var country: Country)

    object Presidents : IdTable<String>("presidents") {
        override val id: Column<EntityID<String>> = varchar("id", 20).entityId()
        val name = varchar("name", 20)
        val governmentYears = varchar("government_years", 20)
        val country = varchar("country", 20)
        override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
    }

    private fun Database.createPresident(president: President) {
        return transaction(this) {
            Presidents.insert {
                it[id] = president.id
                it[name] = president.name
                it[governmentYears] = president.governmentYears
                it[country] = president.country.id
            }
        }
    }

    private fun Database.createLanguage(language: CountryLanguage) {
        return transaction(this) {
            CountryLanguages.insert {
                it[id] = language.id
                it[name] = language.name
                it[country] = language.country.id
            }
        }
    }

    private fun Database.createCountry(country: Country) {
        return transaction(this) {
            Countries.insert {
                it[id] = country.id
                it[name] = country.name
                it[area] = country.area
            }
        }
    }
}
