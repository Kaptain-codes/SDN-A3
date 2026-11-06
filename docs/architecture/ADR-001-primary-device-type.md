# ADR-001: Design for shared laptops and desktops first

* **Date:** 06 Nov 2026
* **Author:** Benito Iyambo, 225005794, Role 3 (Architect, views)
* **Requirements affected:** QR2 (amended); FR1 to FR6 (unchanged)

## Context

A1 and A2 said that facilitators use shared, low-specification devices, but never said what kind of device. A3 built a Java desktop client (STEMCraft) and a separate web prototype, and testers asked whether the product was an app or a website (finding F11 in the A3 report). "Shared" does not mean the system must be designed for every device type, so the team needs a primary device type. The constraints are intermittent internet, costly data, shared devices, and limited technical support.

## Options considered

|Option|Short description|
|-|-|
|A|Laptop and desktop first: design and test for shared Windows laptops; phones and tablets are a secondary, later path|
|B|Mobile first: design for smartphones and tablets; laptops secondary|
|C|Equal support for both from the start|

## Evidence

* As reported in the team's evidence brief: Jatileni and Jatileni (2018), a master's thesis on 90 primary teachers in Omusati Region, reports laptops used by 92.2% (83 of 90), smartphones by 73.3% (66 of 90) and projectors by 70.0% (63 of 90).
* As reported in the team's evidence brief: Osakwe, Ujakpa, Iyawa and Florich (2019), an IST-Africa conference paper on 72 high-school teachers in Erongo, Khomas and Otjozondjupa, reports that teachers already use mobile devices for academic purposes.
* A University of Namibia thesis on remote Khomas schools reports poor classroom internet, a shortage of ICT tools and a lack of electricity as barriers: Ndakolo, E. N. (2023). *Teachers' creation of teaching and learning materials using information communication technology at selected remote schools in the Khomas region, Namibia* \[Master's thesis, University of Namibia]. https://repository.unam.edu.na/handle/11070/3733
* **What this evidence does not show:** the figures are overlapping "used or had used" answers, not frequency; they cover general teaching, not STEM; and they come from selected regions and samples, so they do not prove laptops are the most-used device in every Namibian school.
* The A3 build is a Java desktop client with a repository pattern, tested on Windows laptops.

## Decision

Choose Option A. The primary device is a shared Windows laptop or desktop. Phones and tablets are a secondary path that is documented but not built or tested in A4.

## Consequences

* **Benefits:** reuses the A3 code; keeps the A4 vertical slice small and testable; matches the strongest available device evidence.
* **Costs and trade-offs:** facilitators who only have a phone are not served by this version.
* **Residual risks:** the device evidence is limited (selected regions, general teaching). Revisit the decision if evaluation or new evidence shows phone-first use.

## Requirement and traceability changes

* **QR2 (amended):** "Sync uses small batches and no large assets, and the client is usable on low-specification shared laptops and desktops (the primary device), with a warning at logout if items are unsynced." Phone and tablet use is not an A4 requirement. The logout warning stays out of scope in the current build because there is no sign-in (A3 report, D1.5); this ADR does not change that.
* **Change log:** the QR2 amendment is logged with this ADR as its evidence.
* **Traceability:** QR2 links to the deployment view (shared laptop) and to the verification checks below.

## Verification

Run tasks T1 to T4 on a shared Windows laptop. Add a low-specification device check, for example running T1 to T4 on the oldest laptop available and noting start-up time.

