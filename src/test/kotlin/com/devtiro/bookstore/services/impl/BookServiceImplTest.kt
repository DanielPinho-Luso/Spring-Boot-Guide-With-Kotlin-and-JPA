package com.devtiro.bookstore.services.impl

import com.devtiro.bookstore.BOOK_A_ISBN
import com.devtiro.bookstore.domain.AuthorSummary
import com.devtiro.bookstore.domain.BookUpdateRequest
import com.devtiro.bookstore.repositories.AuthorRepository
import com.devtiro.bookstore.repositories.BookRepository
import com.devtiro.bookstore.testAuthorEntityA
import com.devtiro.bookstore.testBookEntityA
import com.devtiro.bookstore.testBookSummaryA
import com.devtiro.bookstore.testBookSummaryB
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class BookServiceImplTest @Autowired constructor(
    private val underTest: BookServiceImpl,
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository
) {

    @Test
    fun `test that createUpdate throws IllegalStateException when author does not exist`() {
        val authorSummary = AuthorSummary(id = 999L)
        val bookSummary = testBookSummaryA(BOOK_A_ISBN, authorSummary)
        assertThrows<IllegalStateException> {
            underTest.createUpdate(BOOK_A_ISBN, bookSummary)
        }
    }

    @Test
    fun `test that createUpdate successfully creates book in the database`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val authorSummary = AuthorSummary(id = savedAuthor.id!!)
        val bookSummary = testBookSummaryA(BOOK_A_ISBN, authorSummary)
        val (savedBook, isCreated) = underTest.createUpdate(BOOK_A_ISBN, bookSummary)
        assertThat(savedBook).isNotNull

        val recalledBook = bookRepository.findByIdOrNull(BOOK_A_ISBN)
        assertThat(recalledBook).isNotNull
        assertThat(recalledBook).isEqualTo(savedBook)
        assertThat(isCreated).isTrue
    }

    @Test
    fun `test that createUpdate successfully updates book in the database`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val authorSummary = AuthorSummary(id = savedAuthor.id!!)
        val bookSummary = testBookSummaryB(BOOK_A_ISBN, authorSummary)
        val (updatedBook, isCreated) = underTest.createUpdate(BOOK_A_ISBN, bookSummary)
        assertThat(updatedBook).isNotNull

        val recalledBook = bookRepository.findByIdOrNull(BOOK_A_ISBN)
        assertThat(recalledBook).isNotNull
        assertThat(recalledBook).isEqualTo(updatedBook)
        assertThat(recalledBook).isEqualTo(savedBook)
        assertThat(isCreated).isFalse
    }

    @Test
    fun `test that list returns an empty list when no books in the database`() {
        val result = underTest.list()
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that list returns books when books in the database`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val result = underTest.list()
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(savedBook)
    }

    @Test
    fun `test that list returns no books when the author ID does not match`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val result = underTest.list(authorId = savedAuthor.id?.let { it + 1 })
        assertThat(result).hasSize(0)
    }

    @Test
    fun `test that list returns books when the author ID does match`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val result = underTest.list(authorId = savedAuthor.id)
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(savedBook)
    }

    @Test
    fun `test that get returns null when book not found in the database`() {
        val result = underTest.get(BOOK_A_ISBN)
        assertThat(result).isNull()
    }

    @Test
    fun `test that get returns book when found in the database`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val result = underTest.get(savedBook.isbn)
        assertThat(result).isNotNull
    }

    @Test
    fun `test that partialUpdate throws IllegalStateException when the book does not exist in the database`() {
        assertThrows<IllegalStateException> {
            val bookUpdateRequest = BookUpdateRequest(title = "A New Title")
            underTest.partialUpdate(BOOK_A_ISBN, bookUpdateRequest)
        }
    }

    @Test
    fun `test that partialUpdate updates the title of an existing book`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val newValue = "A New Title"
        val bookUpdateRequest = BookUpdateRequest(title = newValue)
        val result = underTest.partialUpdate(BOOK_A_ISBN, bookUpdateRequest)
        assertThat(result.title).isEqualTo(newValue)
    }

    @Test
    fun `test that partialUpdate updates the description of an existing book`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val newValue = "A new description."
        val bookUpdateRequest = BookUpdateRequest(description = newValue)
        val result = underTest.partialUpdate(BOOK_A_ISBN, bookUpdateRequest)
        assertThat(result.description).isEqualTo(newValue)
    }

    @Test
    fun `test that partialUpdate updates the image of an existing book`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        val newValue = "new-image.jpeg"
        val bookUpdateRequest = BookUpdateRequest(image = newValue)
        val result = underTest.partialUpdate(BOOK_A_ISBN, bookUpdateRequest)
        assertThat(result.image).isEqualTo(newValue)
    }

    @Test
    fun `test that delete successfully deletes a book in the database`() {
        val savedAuthor = authorRepository.save(testAuthorEntityA())
        assertThat(savedAuthor).isNotNull

        val savedBook = bookRepository.save(testBookEntityA(BOOK_A_ISBN, savedAuthor))
        assertThat(savedBook).isNotNull

        underTest.delete(BOOK_A_ISBN)
        val result = bookRepository.findByIdOrNull(BOOK_A_ISBN)
        assertThat(result).isNull()
    }

    @Test
    fun `test that delete successfully deletes a book that does not exist in the database`() {
        underTest.delete(BOOK_A_ISBN)
        val result = bookRepository.findByIdOrNull(BOOK_A_ISBN)
        assertThat(result).isNull()
    }

}