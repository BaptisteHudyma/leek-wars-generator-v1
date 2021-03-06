# Leek Wars Generator v1
Leek Wars fight generator first version using [leekscript-v1](https://github.com/leek-wars/leekscript-v1) language.

Version modifiée pour permettre l'entrainement de coefficients par algo génétique.

Il faut placer son IA avec un fichier ("coeff.leeks" par exemple) a part qui contient tous les tableaux de nombre a entraîner, puis créer une liste de scénarios qui vont servir a l’entraînement, et simplement lancer le programme.

A la fin du process, on récupère un fichier "best_score.leek" qui a la même structure que le "coeff.leeks" original, avec ses coefficients entraînés par le programme.

Features:
- Parsing automatique d'un fichier d'IA pour entrainer un vecteur de nombres
- Entraînement contre N scénarios

Les améliorations possible:
- Entraînement contre IA qui évolue (éviter l'overfitting)
- Entraînement contre random scenarios et random stats (éviter l'overfitting)
- Contraindre l'évolution des coefficients (forcer le signe ou les valeurs min/max)
- Optimisation ?


Parameters:
- Di Iterations
- Dp Population size
- Dc Coefficient file

## Build
```
gradle jar
```

## AI Training task
Will train the file "scores.leek" for 100 iterations with a genetic pop of 10 with the all the scenarios in test/scenario/
```
java -Di="100" -Dp="10" -Dc=test/ai/myProg/scores.leek -jar generator.ja
```

## Credits
Developed by Dawyde & Pilow © 2012-2019
