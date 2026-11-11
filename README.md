# Member 6 - Reviews

This is the Reviews module extracted from the BikeRentalSystem project. The team plan assigns Member 6 the Review model, DTO, file handler, service, controller, review pages, and review data. This bundle follows the current project implementation and keeps the original source paths.

## Included files

- ReviewController, ReviewService, ReviewFileHandler, and Review model.
- ReviewDTO and ReviewResponse.
- Customer, admin, and edit review pages.
- The current review.txt data file.
- RecordIds.java, a shared helper required by ReviewFileHandler.
- application-review.properties.example, containing the required property line.

## Add it to the project

1. Copy the bundle's src directory into the Spring Boot project, merging folders while preserving the existing package paths.
2. Add the property from application-review.properties.example to the project's application.properties. Do not replace the existing application.properties.
3. Keep the existing BookingService and BikeService. ReviewService calls BookingService.getBookingByUserId to check for a Completed booking and BikeService.getBikeById to confirm the bike exists.
4. The controller expects a User in the session under loggedInUser and the roles Customer and Admin.
5. If RecordIds.java already exists in the target project, keep the existing shared copy rather than duplicating it.

This is a module package, not a standalone Spring Boot application: it depends on the host project's account, bike, and booking modules.

## Existing project integration points

The source project already contains the surrounding integration:

- The shared navigation has customer and admin review links.
- BookingService and BookingController support marking a booking Completed, which enables a review.
- UserController calls ReviewService.deleteReviewsOfUser when removing an account.

When integrating with a different target project, check that these links and integrations exist there.

## Scope note

The team plan's Member 6 section is on page 14. Its example uses a separate addReview page; the current project handles adding a review in reviews/customer.html and also has admin.html and edit.html. The included paths and class names match the current project.