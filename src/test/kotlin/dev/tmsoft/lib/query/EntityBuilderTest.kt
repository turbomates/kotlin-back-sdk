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

    private val database: Database

    init {
        database = Database.connect(h2DatabaseUrl, driver = h2Driver, user = h2User, password = h2Password)
    }

    @Test
    fun `build entity with non-existent root table`() {
        val countries = expectedDataRelativelyPresident()
        transactionalOperations {
            countries.forEach { country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertFailsWith<IllegalArgumentException>(
                message = "The corresponding table is not present in the resulting query",
                block = {
                    getEntity(ExtraneousTable, { toExtraneousEntity() })
                }
            )
        }
    }

    @Test
    fun `build root only`() {
        val countries = expectedDataRelativeCountry()
        transactionalOperations {
            countries.forEach { country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertEquals(getCountriesData(), getEntity(Countries, { toCountry() }))
        }
    }

    @Test
    fun `build three-level nesting object`() {
        val dependencyMapper: Map<IdTable<*>, ResultRow.(Country) -> Country> = mapOf(
            CountryLanguages to { country -> country.apply { countryLanguages.add(toCountryLanguage()) } },
            Presidents to { country -> country.apply { presidents.add(toPresident()) } },
            Facts to { country ->
                val fact: Fact = toFact()
                country.presidents.filter { it.id == fact.presidentId }.map { it.facts.add(fact) }
                country
            }
        )
        val countries = expectedDataRelativeCountry()
        transactionalOperations {
            countries.forEach { country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }

                country.presidents.forEach { president ->
                    database.createPresident(president)
                    president.facts.forEach { fact ->
                        database.createFact(fact)
                    }
                }
            }
            assertEquals(countries, getEntity(Countries, { toCountry() }, dependencyMapper))
        }
    }

    @Test
    fun `build less dependencies then in resulting row`() {
        val dependencyMapper: Map<IdTable<*>, ResultRow.(Country) -> Country> = mapOf(
            CountryLanguages to { country -> country.apply { countryLanguages.add(toCountryLanguage()) } }
        )
        val (belgium, belarus) = getCountriesData()
        belarus.countryLanguages = getBelarusLanguages(belarus)
        belgium.countryLanguages = getBelgiumLanguages(belgium)
        transactionalOperations {
            expectedDataRelativeCountry().forEach { country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertEquals(
                listOf(belgium, belarus),
                getEntity(Countries, { toCountry() }, dependencyMapper)
            )
        }
    }

    @Test
    fun `build entity mapped not to main table`() {
        val countries = expectedDataRelativelyPresident()
        val dependencyMapper: Map<IdTable<*>, ResultRow.(President) -> President> = mapOf(
            Countries to { president -> president.apply { country = toCountry() } },
            CountryLanguages to { president -> president.apply { country.countryLanguages.add(toCountryLanguage()) } }
        )
        transactionalOperations {
            countries.forEach { country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertEquals(
                countries.flatMap { it.presidents },
                getEntity(Presidents, { toPresident() }, dependencyMapper)
            )
        }
    }

    @Test
    fun `build entity with non-existent dependency table`() {
        val countries = expectedDataRelativelyPresident()
        val dependencyMapper: Map<IdTable<*>, ResultRow.(Country) -> Country> = mapOf(
            CountryLanguages to { country -> country.apply { countryLanguages.add(toCountryLanguage()) } },
            ExtraneousTable to { country -> country.apply { toExtraneousEntity() } }
        )
        transactionalOperations {
            countries.forEach { country ->
                database.createCountry(country)
                country.countryLanguages.forEach { database.createLanguage(it) }
                country.presidents.forEach { database.createPresident(it) }
            }
            assertFailsWith<IllegalArgumentException>(
                message = "The corresponding table is not present in the resulting query",
                block = {
                    getEntity(Countries, { toCountry() }, dependencyMapper)
                }
            )
        }
    }

    private fun <T> getEntity(
        rootTable: IdTable<*>,
        rootMapper: ResultRow.() -> T,
        dependencyMapper: Map<IdTable<*>, ResultRow.(T) -> T> = emptyMap()
    ): List<T> {
        return Countries
            .join(Presidents, JoinType.LEFT, Presidents.country, Countries.id)
            .join(CountryLanguages, JoinType.LEFT, CountryLanguages.country, Countries.id)
            .join(Facts, JoinType.LEFT, Facts.presidentId, Presidents.id)
            .selectAll()
            .fold(rootTable, rootMapper, dependencyMapper)
    }

    private fun expectedDataRelativeCountry(): List<Country> {
        val (belgium, belarus) = getCountriesData()
        val belarusPresident = getBelarusPresidents(belarus)
        val belgiumLanguages = getBelgiumLanguages(belgium)
        val belarusLanguages = getBelarusLanguages(belarus)
        val factsBelarusPresidents = getFactsData()
        belarusPresident.forEach { president ->
            factsBelarusPresidents.forEach { fact ->
                if (president.id == fact.presidentId) {
                    president.facts.add(fact)
                }
            }
        }

        belarus.presidents = belarusPresident
        belgium.countryLanguages = belgiumLanguages
        belarus.countryLanguages = belarusLanguages

        return listOf(belgium, belarus)
    }

    private fun expectedDataRelativelyPresident(): List<Country> {
        val (belgium, belarus) = getCountriesData()

        belgium.countryLanguages = getBelgiumLanguages(belgium)
        belarus.countryLanguages = getBelarusLanguages(belarus)
        belarus.presidents = getBelarusPresidents(belarus)

        return listOf(belgium, belarus)
    }

    private fun getCountriesData(): List<Country> {
        return listOf(Country("2", "Belgium", "30.689"), Country("1", "Belarus", "207.6"))
    }

    private fun getFactsData(): List<Fact> {
        return listOf(Fact("1", "Some fact 1", "1"), Fact("2", "Some fact 2", "1"))
    }

    private fun getBelarusPresidents(country: Country): MutableList<President> {
        return mutableListOf(
            President("1", "Tihanovskaya", "2020 - till now", country.copy()),
            President("2", "Lukashenko", "1994 - 2020", country.copy())
        )
    }

    private fun getBelgiumLanguages(belgium: Country): MutableList<CountryLanguage> {
        return mutableListOf(
            CountryLanguage("3", "Dutch", belgium.copy()),
            CountryLanguage("4", "French", belgium.copy()), CountryLanguage("5", "German", belgium.copy())
        )
    }

    private fun getBelarusLanguages(belarus: Country): MutableList<CountryLanguage> {
        return mutableListOf(
            CountryLanguage("1", "Russian", belarus.copy()),
            CountryLanguage("2", "Belorussian", belarus.copy())
        )
    }

    private fun ResultRow.toPresident(): President {
        return President(
            this[Presidents.id].value,
            this[Presidents.name],
            this[Presidents.governmentYears],
            Country(this[Presidents.country], this[Countries.name], this[Countries.area])
        )
    }

    private fun ResultRow.toFact(): Fact {
        return Fact(
            this[Facts.id].value,
            this[Facts.info],
            this[Facts.presidentId]
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

    data class Fact(
        val id: String,
        val info: String,
        val presidentId: String
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

    data class President(
        val id: String,
        val name: String,
        val governmentYears: String,
        var country: Country,
        val facts: MutableList<Fact> = mutableListOf()
    )

    object Presidents : IdTable<String>("presidents") {
        override val id: Column<EntityID<String>> = varchar("id", 20).entityId()
        val name = varchar("name", 20)
        val governmentYears = varchar("government_years", 20)
        val country = varchar("country", 20)
        override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
    }

    object Facts : IdTable<String>("facts") {
        override val id: Column<EntityID<String>> = varchar("id", 20).entityId()
        val info = varchar("info", 20)
        val presidentId = varchar("president_id", 20)
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

    private fun Database.createFact(fact: Fact) {
        return transaction(this) {
            Facts.insert {
                it[id] = fact.id
                it[info] = fact.info
                it[presidentId] = fact.presidentId
            }
        }
    }

    private fun transactionalOperations(operation: () -> Unit) {
        transaction {
            SchemaUtils.create(Countries, CountryLanguages, Presidents, Facts)
            operation()
        }
    }
}
