package com.picnicml.doddlemodel.linear

import breeze.linalg.sum
import breeze.numerics.{log, sigmoid}
import com.picnicml.doddlemodel.data.{Features, RealVector, Simplex, Target}

/** An immutable multiple logistic regression model with ridge regularization.
  *
  * @param lambda L2 regularization strength, must be positive, 0 means no regularization
  *
  * Examples:
  * val model = LogisticRegression()
  * val model = LogisticRegression(lambda = 1.5)
  */
@SerialVersionUID(1L)
class LogisticRegression private (val lambda: Double, val numClasses: Option[Int], protected val w: Option[RealVector])
  extends LinearClassifier[LogisticRegression] with Serializable {

  private var yPredProbaCache: Option[RealVector] = None

  override protected def copy(numClasses: Int): LogisticRegression = {
    require(numClasses == 2, "Logistic regression must be trained on a dataset with exactly 2 categories")
    new LogisticRegression(this.lambda, Some(numClasses), this.w)
  }

  override protected def copy(w: RealVector): LogisticRegression =
    new LogisticRegression(this.lambda, this.numClasses, Some(w))

  override protected def predict(w: RealVector, x: Features): Target =
    (this.predictProba(w, x)(::, 0) >:> 0.5).map(x => if (x) 1.0 else 0.0)

  override protected def predictProba(w: RealVector, x: Features): Simplex =
    sigmoid(x * w).asDenseMatrix.t

  override protected[linear] def loss(w: RealVector, x: Features, y: Target): Double = {
    yPredProbaCache = Some(this.predictProba(w, x)(::, 0))
    sum(y * log(yPredProbaCache.get) + (1.0 - y) * log(1.0 - yPredProbaCache.get)) / (-x.rows.toDouble) +
      .5 * this.lambda * (w(1 to -1).t * w(1 to -1))
  }

  override protected[linear] def lossGrad(w: RealVector, x: Features, y: Target): RealVector = {
    val grad = ((y - yPredProbaCache.get).t * x).t / (-x.rows.toDouble)
    yPredProbaCache = None
    grad(1 to -1) += this.lambda * w(1 to -1)
    grad
  }
}

object LogisticRegression {

  def apply(): LogisticRegression = new LogisticRegression(0, None, None)

  def apply(lambda: Double): LogisticRegression = {
    require(lambda >= 0, "L2 regularization strength must be positive")
    new LogisticRegression(lambda, None, None)
  }
}
