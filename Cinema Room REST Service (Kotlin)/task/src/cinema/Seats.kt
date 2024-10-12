package cinema

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
class CustomErrorMessage (
    val statusCode: Int? = null,
    val error: String
)

class TokenExpiredException : RuntimeException()
class WrongPassword : RuntimeException()

@ControllerAdvice
class OutOfBoundsHandler {
    @ExceptionHandler(IndexOutOfBoundsException::class)
    fun handleSeatOutOfRange(
        ex: IndexOutOfBoundsException, request: WebRequest
    ): ResponseEntity<CustomErrorMessage> {
        val body = CustomErrorMessage(
            statusCode = 400,
            error = "The number of a row or a column is out of bounds!"
        )
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleTicketAlreadyPurchased(
        ex: ResponseStatusException, request: WebRequest
    ): ResponseEntity<CustomErrorMessage> {
        val body = CustomErrorMessage(
            statusCode = 500,
            error = "The ticket has been already purchased!"
        )
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(TokenExpiredException::class)
    fun handleWrongPassword(
        ex: TokenExpiredException, request: WebRequest
    ): ResponseEntity<CustomErrorMessage> {
        val body = CustomErrorMessage(error = "Wrong token!")
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(WrongPassword::class)
    fun handleTokenExpired(
        ex: WrongPassword, request: WebRequest
    ): ResponseEntity<CustomErrorMessage> {
        val body = CustomErrorMessage(error = "The password is wrong!")
        return ResponseEntity(body, HttpStatus.UNAUTHORIZED)
    }
}

    class Seat(val row: Int, val column: Int, val price: Int, @JsonIgnore var free: Boolean = true)


    data class Seats(
        val total_rows: Int = 9,
        val total_columns: Int = 9,
        val available_seats: MutableList<Seat>
    )

    data class Token(
        val token: UUID
    )


    data class PurchaseResponse(
        var token: UUID,
        val ticket: Seat
    )


    data class ReturnedTicket(
        val returned_ticket: Seat
    )

    data class Stats(
        val current_income: Int,
        val number_of_available_seats: Int,
        val number_of_purchased_tickets: Int
    )

    @RestController
    class SeatsController {
        val seatList = mutableListOf<Seat>()
        var purchase: PurchaseResponse? = null

        init {
            var price = 10
            for (i in 1..9) {
                for (j in 1..9) {
                    if (i > 4) price = 8
                    val seat = Seat(i, j, price)
                    seatList.add(seat)
                }
            }
        }

        @GetMapping(path = ["/seats"])
        fun getSeats(): Seats {
            return Seats(available_seats = seatList)
        }

        @PostMapping("/purchase")
        fun purchase(@RequestBody ticket: Seat): PurchaseResponse? {
            val ListOfSeats = getSeats().available_seats
            val x =
                getSeats().available_seats.indexOf(getSeats().available_seats.find { it.row == ticket.row && it.column == ticket.column })

            if (ListOfSeats[x].free == false) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST
                )
            }

            val uuid: UUID = UUID.randomUUID()

            ListOfSeats[x] = Seat(ticket.row, ticket.column, ListOfSeats[x].price, false)
            purchase = PurchaseResponse(uuid, ListOfSeats[x])

            return purchase
        }

        @PostMapping("/return")
        fun refund(@RequestBody token: Token): ReturnedTicket? {

            if (purchase?.token != token.token) {
                throw TokenExpiredException()
            }
            purchase?.ticket?.free = true
            purchase?.token = UUID(0, 0)
            return ReturnedTicket(purchase!!.ticket)
        }

        @GetMapping("/stats")

        fun stats(@RequestParam(required = false) password: String?): Stats {
            var currentIncome = 0
            if (password == null || password != "super_secret") {
                throw WrongPassword()
            }

            for (i in seatList) {
                if (i.free == false) {
                    currentIncome += i.price
                }
            }

            return Stats(
                current_income = currentIncome,
                number_of_available_seats = seatList.count { it.free == true },
                number_of_purchased_tickets = seatList.count { it.free == false })

        }
    }















