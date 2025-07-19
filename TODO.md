# Remaining features to implement
The following is a list os feature I would like to implement, along with an example prompt that can be used by the AI agent to implement such feature.

1. Add location selection + offer an auto-detect setting
Prompt: I have an Android weather map based on meteo.gc.ca website data. For now we try to detect the current device location and fallback to an  │
   │    hardcoded location if we cannot detect properly the current location. I would like to change that behaviour so that we start with the     │
   │    Montreal hardcoded location, and do not yet ask for the coarsed location permission. Add a way to switch the current location, via a long │
   │     press on the "Location: Montreal" part. This should bring a modal (or a complete new page, if modal is not possible), and offer to       │
   │    select between Montreal or "Detect my current location". The choice should be persisted as a preference. If "Detect my current location"  │
   │    is selected, then use the coarse location permission and fetch the current location. Let me know if you need more details to accomplish   │
   │    this.
2. Add search functionality to find another city, and remove the hardcoded Montreal city
3. Add possibility to build a list of cities we can switch between. We should be able to also remove a city from the list
4. In the radar map pre-fetch the radar images, so when we playback it goes smoother
5. Add a settings for the radar map to select between a 3h or 1h radar history for the playback
6. Refresh the weather data when we drag down the app`s screen. Should work for both the forecast page, and the radar map page
7. Allow possibility to move and zoom the Radar map
8. Display the current radar map timestamp
