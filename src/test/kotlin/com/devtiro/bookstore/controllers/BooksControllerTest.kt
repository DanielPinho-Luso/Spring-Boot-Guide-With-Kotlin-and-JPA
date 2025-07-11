package com.devtiro.bookstore.controllers

import com.devtiro.bookstore.BOOK_A_ISBN
import com.devtiro.bookstore.domain.BookUpdateRequest
import com.devtiro.bookstore.domain.dto.BookUpdateRequestDto
import com.devtiro.bookstore.services.BookService
import com.devtiro.bookstore.testAuthorEntityA
import com.devtiro.bookstore.testAuthorSummaryDtoA
import com.devtiro.bookstore.testBookEntityA
import com.devtiro.bookstore.testBookSummaryDtoA
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.result.StatusResultMatchersDsl

@SpringBootTest
@AutoConfigureMockMvc
class BooksControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    @MockkBean val bookService: BookService
) {
    val objectMapper = ObjectMapper()

    @Test
    fun `test that createFullUpdateBook returns HTTP 201 when book is created`() {
        assertThatUserCreatedUpdated(true) { isCreated() }
    }

    @Test
    fun `test that createFullUpdateBook returns HTTP 200 when book is updated`() {
        assertThatUserCreatedUpdated(false) { isOk() }
    }

    private fun assertThatUserCreatedUpdated(
        isCreated: Boolean,
        statusCodeAssertion: StatusResultMatchersDsl.() -> Unit
    ) {
        val isbn = "978-089-230342-0777"
        val author = testAuthorEntityA(id = 1)
        val savedBook = testBookEntityA(isbn, author)

        val authorSummaryDto = testAuthorSummaryDtoA(id = 1)
        val bookSummaryDto = testBookSummaryDtoA(isbn, authorSummaryDto)

        every {
            bookService.createUpdate(isbn, any())
        } answers {
            Pair(savedBook, isCreated)
        }

        mockMvc.put("/v1/books/${isbn}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookSummaryDto)
        }.andExpect {
            status { statusCodeAssertion() }
        }
    }

    @Test
    fun `test that createFullUpdateBook returns HTTP 500 when author in the database does not have an ID`() {
        val isbn = "978-089-230342-0777"
        val author = testAuthorEntityA()
        val savedBook = testBookEntityA(isbn, author)

        val authorSummaryDto = testAuthorSummaryDtoA(id = 1)
        val bookSummaryDto = testBookSummaryDtoA(isbn, authorSummaryDto)

        every {
            bookService.createUpdate(isbn, any())
        } answers {
            Pair(savedBook, true)
        }

        mockMvc.put("/v1/books/${isbn}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookSummaryDto)
        }.andExpect {
            status { isInternalServerError() }
        }
    }

    @Test
    fun `test that createFullUpdateBook returns HTTP 400 when author does not exist`() {
        val isbn = "978-089-230342-0777"

        val authorSummaryDto = testAuthorSummaryDtoA(id = 1)
        val bookSummaryDto = testBookSummaryDtoA(isbn, authorSummaryDto)

        every {
            bookService.createUpdate(isbn, any())
        } throws IllegalStateException()

        mockMvc.put("/v1/books/${isbn}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookSummaryDto)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `test that readManyBooks returns a list of books`() {
        every {
            bookService.list()
        } answers {
            listOf(testBookEntityA(isbn = BOOK_A_ISBN, testAuthorEntityA(id = 1)))
        }

        mockMvc.get("/v1/books") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { jsonPath("$[0].isbn", equalTo(BOOK_A_ISBN)) }
            content { jsonPath("$[0].title", equalTo("Test Book A")) }
            content { jsonPath("$[0].image", equalTo("book-image.jpeg")) }
            content { jsonPath("$[0].author.id", equalTo(1)) }
            content { jsonPath("$[0].author.name", equalTo("John Doe")) }
            content { jsonPath("$[0].author.image", equalTo("author-image.jpeg")) }
        }
    }

    @Test
    fun `test that list returns no books when they do not match the author ID`() {
        every {
            bookService.list(authorId = any())
        } answers {
            emptyList()
        }

        mockMvc.get("/v1/books?author=999") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { json("[]") }
        }
    }

    @Test
    fun `test that list returns book when matches the author ID`() {
        every {
            bookService.list(authorId = 1L)
        } answers {
            listOf(
                testBookEntityA(
                    isbn = BOOK_A_ISBN,
                    author = testAuthorEntityA(id = 1L)
                )
            )
        }

        mockMvc.get("/v1/books?author=1") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { jsonPath("$[0].isbn", equalTo(BOOK_A_ISBN)) }
            content { jsonPath("$[0].title", equalTo("Test Book A")) }
            content { jsonPath("$[0].image", equalTo("book-image.jpeg")) }
            content { jsonPath("$[0].author.id", equalTo(1)) }
            content { jsonPath("$[0].author.name", equalTo("John Doe")) }
            content { jsonPath("$[0].author.image", equalTo("author-image.jpeg")) }
        }
    }

    @Test
    fun `test that readOneBook returns HTTP 404 when no book found`() {
        every {
            bookService.get(any())
        } answers {
            null
        }

        mockMvc.get("/v1/books/$BOOK_A_ISBN") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `test that readOneBook returns book and HTTP 200 when found`() {
        every {
            bookService.get(BOOK_A_ISBN)
        } answers {
            testBookEntityA(isbn = BOOK_A_ISBN, author = testAuthorEntityA(id = 1))
        }

        mockMvc.get("/v1/books/$BOOK_A_ISBN") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { jsonPath("$.isbn", equalTo(BOOK_A_ISBN)) }
            content { jsonPath("$.title", equalTo("Test Book A")) }
            content { jsonPath("$.image", equalTo("book-image.jpeg")) }
            content { jsonPath("$.author.id", equalTo(1)) }
            content { jsonPath("$.author.name", equalTo("John Doe")) }
            content { jsonPath("$.author.image", equalTo("author-image.jpeg")) }
        }
    }

    @Test
    fun `test that bookPartialUpdate returns HTTP 400 on IllegalStateException`() {
        val newValue = "A New Title"
        val bookUpdateRequest = BookUpdateRequest(
            title = newValue
        )

        val bookUpdateRequestDto = BookUpdateRequestDto(
            title = newValue
        )

        every {
            bookService.partialUpdate(BOOK_A_ISBN, bookUpdateRequest)
        } throws IllegalStateException()

        mockMvc.patch("/v1/books/$BOOK_A_ISBN") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookUpdateRequestDto)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `test that bookPartialUpdate returns HTTP 200 and book on successful update`() {
        val newValue = "A New Title"
        val bookUpdateRequest = BookUpdateRequest(title = newValue)
        val bookUpdateRequestDto = BookUpdateRequestDto(title = newValue)
        val bookEntity = testBookEntityA(BOOK_A_ISBN, testAuthorEntityA(id = 1))

        every {
            bookService.partialUpdate(BOOK_A_ISBN, bookUpdateRequest)
        } answers {
            bookEntity
        }

        mockMvc.patch("/v1/books/$BOOK_A_ISBN") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookUpdateRequestDto)
        }.andExpect {
            status { isOk() }
            content { jsonPath("$.isbn", equalTo(BOOK_A_ISBN)) }
            content { jsonPath("$.title", equalTo("Test Book A")) }
            content { jsonPath("$.image", equalTo("book-image.jpeg")) }
            content { jsonPath("$.author.id", equalTo(1)) }
            content { jsonPath("$.author.name", equalTo("John Doe")) }
            content { jsonPath("$.author.image", equalTo("author-image.jpeg")) }
        }
    }

    @Test
    fun `test that deleteBook deletes a book successfully`() {
        every {
            bookService.delete(BOOK_A_ISBN)
        } answers {}

        mockMvc.delete("/v1/books/$BOOK_A_ISBN") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNoContent() }
        }
    }

}